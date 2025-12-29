package im.bigs.pg.application.payment.service

import im.bigs.pg.application.payment.port.`in`.QueryFilter
import im.bigs.pg.application.payment.port.`in`.QueryPaymentsUseCase
import im.bigs.pg.application.payment.port.`in`.QueryResult
import im.bigs.pg.application.payment.port.out.PaymentOutPort
import im.bigs.pg.application.payment.port.out.PaymentQuery
import im.bigs.pg.application.payment.port.out.PaymentSummaryFilter
import im.bigs.pg.domain.payment.PaymentStatus
import im.bigs.pg.domain.payment.PaymentSummary
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Base64

/**
 * 결제 이력 조회 유스케이스 구현체.
 * - 커서 토큰은 createdAt/id를 안전하게 인코딩해 전달/복원합니다.
 * - 통계는 조회 조건과 동일한 집합을 대상으로 계산됩니다.
 */
@Service
class QueryPaymentsService(
    private val paymentRepository: PaymentOutPort,
) : QueryPaymentsUseCase {
    /**
     * 필터를 기반으로 결제 내역을 조회합니다.
     *
     * @param filter 파트너/상태/기간/커서/페이지 크기
     * @return 조회 결과(목록/통계/커서)
     */
    override fun query(filter: QueryFilter): QueryResult {
        val (cursorCreatedAt, cursorId) = decodeCursor(filter.cursor)

        val query = PaymentQuery(
            partnerId = filter.partnerId,
            status = filter.status?.let { PaymentStatus.valueOf(it) },
            from = filter.from,
            to = filter.to,
            cursorCreatedAt = cursorCreatedAt,
            cursorId = cursorId,
            limit = filter.limit,
        )

        val page = paymentRepository.findBy(query)

        val summaryFilter = PaymentSummaryFilter(
            partnerId = filter.partnerId,
            status = filter.status?.let { PaymentStatus.valueOf(it) },
            from = filter.from,
            to = filter.to,
        )
        val summaryProjection = paymentRepository.summary(summaryFilter)

        return QueryResult(
            items = page.items,
            summary = PaymentSummary(
                count = summaryProjection.count,
                totalAmount = summaryProjection.totalAmount,
                totalNetAmount = summaryProjection.totalNetAmount,
            ),
            nextCursor = if (page.hasNext) encodeCursor(page.nextCursorCreatedAt, page.nextCursorId) else null,
            hasNext = page.hasNext,
        )
    }

    /** 다음 페이지 이동을 위한 커서 인코딩. */
    private fun encodeCursor(
        createdAt: LocalDateTime?,
        id: Long?,
    ): String? {
        if (createdAt == null || id == null) return null
        val instant = createdAt.toInstant(ZoneOffset.UTC)
        val raw = "${instant.toEpochMilli()}:$id"
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.toByteArray())
    }

    /** 요청으로 전달된 커서 복원. 유효하지 않으면 null 커서로 간주합니다. */
    private fun decodeCursor(cursor: String?): Pair<LocalDateTime?, Long?> {
        if (cursor.isNullOrBlank()) return null to null
        return try {
            val raw = String(Base64.getUrlDecoder().decode(cursor))
            val parts = raw.split(":")
            val ts = parts[0].toLong()
            val id = parts[1].toLong()
            val instant = Instant.ofEpochMilli(ts)
            LocalDateTime.ofInstant(instant, ZoneOffset.UTC) to id
        } catch (e: Exception) {
            null to null
        }
    }
}

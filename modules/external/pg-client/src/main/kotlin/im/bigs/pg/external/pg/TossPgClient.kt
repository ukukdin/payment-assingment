package im.bigs.pg.external.pg

import im.bigs.pg.application.pg.port.out.PgApproveRequest
import im.bigs.pg.application.pg.port.out.PgApproveResult
import im.bigs.pg.application.pg.port.out.PgClientOutPort
import im.bigs.pg.domain.payment.PaymentStatus
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

/**
 * 토스페이먼츠 스타일 PG 클라이언트 (Mock).
 *
 * - partnerId가 3으로 나누어 떨어지는 경우 이 어댑터 사용
 * - 실제 연동 시 토스페이먼츠 API 스펙에 맞춰 구현
 *
 * 토스페이먼츠 특징:
 * - paymentKey 기반 결제 관리
 * - Secret Key 인증
 * - JSON 기반 RESTful API
 */
@Component
class TossPgClient : PgClientOutPort {

    /**
     * 이 어댑터가 해당 partnerId를 처리할 수 있는지 판단.
     * partnerId가 3으로 나누어 떨어지면 토스페이먼츠 사용.
     */
    override fun supports(partnerId: Long): Boolean = partnerId % 3L == 0L

    /**
     * 결제 승인 처리.
     * Mock 구현으로, paymentKey 형태의 승인 코드를 생성합니다.
     */
    override fun approve(request: PgApproveRequest): PgApproveResult {
        // 토스페이먼츠 스타일의 paymentKey 생성 (실제로는 API 응답에서 받음)
        val paymentKey = generatePaymentKey()

        // 카드 마지막 4자리 추출
        val maskedCardLast4 = request.cardNumber?.replace("-", "")?.takeLast(4)
            ?: request.cardLast4

        return PgApproveResult(
            approvalCode = paymentKey,
            approvedAt = LocalDateTime.now(ZoneOffset.UTC),
            maskedCardLast4 = maskedCardLast4,
            status = PaymentStatus.APPROVED,
        )
    }

    /**
     * 토스페이먼츠 스타일의 paymentKey 생성.
     * 실제로는 API 응답에서 받지만, Mock에서는 UUID 기반으로 생성.
     */
    private fun generatePaymentKey(): String {
        // 토스페이먼츠 paymentKey 형식 (예: tgen_20231229_xxxxxxxx)
        val uuid = UUID.randomUUID().toString().replace("-", "").take(8)
        return "tgen_${java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE)}_$uuid"
    }
}

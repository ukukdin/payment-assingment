package im.bigs.pg.application.payment.port.`in`

import java.time.LocalDateTime

/**
 * 결제 조회 조건.
 * - cursor 는 다음 페이지를 가리키는 토큰(Base64 URL-safe)
 * - 기간은 UTC 기준 권장
 */
data class QueryFilter(
    val partnerId: Long? = null,
    val status: String? = null,
    val from: LocalDateTime? = null,
    val to: LocalDateTime? = null,
    val cursor: String? = null,
    val limit: Int = 20,
)

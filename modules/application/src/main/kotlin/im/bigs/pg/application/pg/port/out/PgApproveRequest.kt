package im.bigs.pg.application.pg.port.out

import java.math.BigDecimal

/** PG 승인 요청 최소 정보.
 * - cardNumber, birthDate, expiry, password는 TestPG API 연동 시 필수
 * - cardBin, cardLast4는 결제 저장용 마스킹 정보
 **/
data class PgApproveRequest(
    val partnerId: Long,
    val amount: BigDecimal,
    val cardNumber: String? = null,
    val birthDate: String? = null,
    val expiry: String? = null,
    val password: String? = null,
    val cardBin: String?,
    val cardLast4: String?,
    val productName: String?,
)

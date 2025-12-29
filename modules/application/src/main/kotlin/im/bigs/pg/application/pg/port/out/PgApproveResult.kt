package im.bigs.pg.application.pg.port.out

import im.bigs.pg.domain.payment.PaymentStatus
import java.time.LocalDateTime

/** PG 승인 결과 요약. */
data class PgApproveResult(
    val approvalCode: String,
    val approvedAt: LocalDateTime,
    val maskedCardLast4: String?,
    val status: PaymentStatus = PaymentStatus.APPROVED,
)

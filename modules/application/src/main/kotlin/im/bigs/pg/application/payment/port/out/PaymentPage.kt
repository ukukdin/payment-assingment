package im.bigs.pg.application.payment.port.out

import im.bigs.pg.domain.payment.Payment
import java.time.LocalDateTime

/** 페이지 결과. */
data class PaymentPage(
    val items: List<Payment>,
    val hasNext: Boolean,
    val nextCursorCreatedAt: LocalDateTime?,
    val nextCursorId: Long?,
)

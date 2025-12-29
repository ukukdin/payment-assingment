package im.bigs.pg.application.payment.port.out

import com.fasterxml.jackson.annotation.JsonFormat
import im.bigs.pg.domain.payment.PaymentStatus
import java.time.LocalDateTime

/** 통계용 필터 – 페이지와 동일 조건 사용 권장. */
data class PaymentSummaryFilter(
    val partnerId: Long? = null,
    val status: PaymentStatus? = null,
    val from: LocalDateTime? = null,
    val to: LocalDateTime? = null,
)

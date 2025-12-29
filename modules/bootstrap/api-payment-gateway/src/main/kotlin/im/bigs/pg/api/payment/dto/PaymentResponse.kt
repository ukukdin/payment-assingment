package im.bigs.pg.api.payment.dto

import com.fasterxml.jackson.annotation.JsonFormat
import im.bigs.pg.domain.payment.Payment
import im.bigs.pg.domain.payment.PaymentStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDateTime

@Schema(description = "결제 응답")
data class PaymentResponse(
    @Schema(description = "결제 ID", example = "1")
    val id: Long?,

    @Schema(description = "제휴사 ID", example = "1")
    val partnerId: Long,

    @Schema(description = "결제 금액", example = "50000")
    val amount: BigDecimal,

    @Schema(description = "적용된 수수료율", example = "0.035")
    val appliedFeeRate: BigDecimal,

    @Schema(description = "수수료 금액", example = "1750")
    val feeAmount: BigDecimal,

    @Schema(description = "정산 금액 (결제 금액 - 수수료)", example = "48250")
    val netAmount: BigDecimal,

    @Schema(description = "카드 마지막 4자리 (마스킹)", example = "3456")
    val cardLast4: String?,

    @Schema(description = "승인 번호", example = "12290001")
    val approvalCode: String,

    @Schema(description = "승인 일시", example = "2025-12-29 10:30:00")
    @get:JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val approvedAt: LocalDateTime,

    @Schema(description = "결제 상태", example = "APPROVED")
    val status: PaymentStatus,

    @Schema(description = "생성 일시", example = "2025-12-29 10:30:00")
    @get:JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(p: Payment) = PaymentResponse(
            id = p.id,
            partnerId = p.partnerId,
            amount = p.amount,
            appliedFeeRate = p.appliedFeeRate,
            feeAmount = p.feeAmount,
            netAmount = p.netAmount,
            cardLast4 = p.cardLast4,
            approvalCode = p.approvalCode,
            approvedAt = p.approvedAt,
            status = p.status,
            createdAt = p.createdAt,
        )
    }
}

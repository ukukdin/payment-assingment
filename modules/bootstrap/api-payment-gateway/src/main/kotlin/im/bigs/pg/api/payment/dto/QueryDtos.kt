package im.bigs.pg.api.payment.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

@Schema(description = "결제 목록 조회 응답")
data class QueryResponse(
    @Schema(description = "결제 목록")
    val items: List<PaymentResponse>,

    @Schema(description = "통계 정보")
    val summary: Summary,

    @Schema(description = "다음 페이지 커서 (다음 페이지가 없으면 null)", example = "eyJpZCI6MTAwfQ==")
    val nextCursor: String?,

    @Schema(description = "다음 페이지 존재 여부", example = "true")
    val hasNext: Boolean,
)

@Schema(description = "결제 통계 정보")
data class Summary(
    @Schema(description = "총 결제 건수", example = "150")
    val count: Long,

    @Schema(description = "총 결제 금액", example = "7500000")
    val totalAmount: BigDecimal,

    @Schema(description = "총 정산 금액 (수수료 제외)", example = "7237500")
    val totalNetAmount: BigDecimal,
)

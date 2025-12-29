package im.bigs.pg.api.payment.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import java.math.BigDecimal

/**
 * 결제 생성 요청.
 *
 * @property partnerId 제휴사 ID
 * @property amount 결제 금액
 * @property cardNumber 카드번호 (하이픈 허용, TestPG 연동 시 필수)
 * @property birthDate 생년월일 YYYYMMDD
 * @property expiry 유효기간 MMYY
 * @property password 카드 비밀번호 앞 2자리
 * @property cardBin 카드 BIN (선택)
 * @property cardLast4 카드 마지막 4자리 (선택)
 * @property productName 상품명 (선택)
 */
@Schema(description = "결제 생성 요청")
data class CreatePaymentRequest(
    @Schema(description = "제휴사 ID", example = "1", required = true)
    val partnerId: Long,

    @Schema(description = "결제 금액 (1 이상)", example = "50000", required = true)
    @field:Min(1)
    val amount: BigDecimal,

    @Schema(description = "카드번호 (하이픈 허용, TestPG 연동 시 필수)", example = "1234-5678-9012-3456")
    val cardNumber: String? = null,

    @Schema(description = "생년월일 YYYYMMDD (TestPG 연동 시 필수)", example = "19900101")
    val birthDate: String? = null,

    @Schema(description = "유효기간 MMYY (TestPG 연동 시 필수)", example = "1226")
    val expiry: String? = null,

    @Schema(description = "카드 비밀번호 앞 2자리 (TestPG 연동 시 필수)", example = "12")
    val password: String? = null,

    @Schema(description = "카드 BIN (앞 6자리)", example = "123456")
    val cardBin: String? = null,

    @Schema(description = "카드 마지막 4자리", example = "3456")
    val cardLast4: String? = null,

    @Schema(description = "상품명", example = "테스트 상품")
    val productName: String? = null,
)

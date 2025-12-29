package im.bigs.pg.external.pg

import im.bigs.pg.application.pg.port.out.PgApproveRequest
import im.bigs.pg.application.pg.port.out.PgApproveResult
import im.bigs.pg.application.pg.port.out.PgClientOutPort
import im.bigs.pg.domain.payment.PaymentStatus
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.UUID

/**
 * 토스페이먼츠 API 연동 클라이언트.
 *
 * - partnerId가 3으로 나누어 떨어지는 경우 이 어댑터 사용 (3, 6, 9, 12...)
 * - Basic Auth 인증 (Secret Key)
 * - 키인결제 API 사용 (POST /v1/payments/key-in)
 *
 * @see <a href="https://docs.tosspayments.com">토스페이먼츠 개발자센터</a>
 */
@Component
class TossPgClient(
    private val restTemplate: RestTemplate,
    @Value("\${tosspayments.secret-key}") private val secretKey: String,
    @Value("\${tosspayments.base-url}") private val baseUrl: String,
) : PgClientOutPort {

    override fun supports(partnerId: Long): Boolean = partnerId % 3L == 0L

    override fun approve(request: PgApproveRequest): PgApproveResult {
        requireNotNull(request.cardNumber) { "토스페이먼츠에는 카드번호(cardNumber)가 필수입니다." }
        requireNotNull(request.expiry) { "토스페이먼츠에는 유효기간(expiry)이 필수입니다." }
        requireNotNull(request.birthDate) { "토스페이먼츠에는 생년월일(birthDate)이 필수입니다." }
        requireNotNull(request.password) { "토스페이먼츠에는 카드 비밀번호 앞 2자리(password)가 필수입니다." }

        val cardNumber = request.cardNumber!!.replace("-", "")
        val expiry = request.expiry!! // MMYY 형식

        // 유효기간 파싱 (MMYY → MM, YY)
        val expirationMonth = expiry.substring(0, 2)
        val expirationYear = expiry.substring(2, 4)

        // 생년월일 파싱 (YYYYMMDD → YYMMDD 또는 사업자번호 10자리)
        val customerIdentityNumber = parseCustomerIdentityNumber(request.birthDate!!)

        val orderId = generateOrderId()
        val orderName = request.productName ?: "결제"

        val headers = createAuthHeaders()
        val body = mapOf(
            "method" to "카드",
            "amount" to request.amount.toInt(),
            "orderId" to orderId,
            "orderName" to orderName,
            "cardNumber" to cardNumber,
            "cardExpirationYear" to expirationYear,
            "cardExpirationMonth" to expirationMonth,
            "cardPassword" to request.password,
            "customerIdentityNumber" to customerIdentityNumber,
        )

        try {
            val response = restTemplate.exchange(
                "$baseUrl/v1/payments/key-in",
                HttpMethod.POST,
                HttpEntity(body, headers),
                TossPaymentResponse::class.java,
            )

            val responseBody = response.body
                ?: throw IllegalStateException("토스페이먼츠로부터 빈 응답을 받았습니다.")

            return PgApproveResult(
                approvalCode = responseBody.paymentKey,
                approvedAt = parseApprovedAt(responseBody.approvedAt),
                maskedCardLast4 = responseBody.card?.number?.takeLast(4)
                    ?: cardNumber.takeLast(4),
                status = mapStatus(responseBody.status),
            )
        } catch (e: HttpClientErrorException.BadRequest) {
            val errorBody = e.responseBodyAsString
            throw IllegalStateException("토스페이먼츠 요청 오류: $errorBody")
        } catch (e: HttpClientErrorException.Unauthorized) {
            throw IllegalStateException("토스페이먼츠 인증 실패: 잘못된 Secret Key")
        } catch (e: HttpClientErrorException.Forbidden) {
            throw IllegalStateException("토스페이먼츠 권한 오류: 키인결제 권한이 없습니다.")
        } catch (e: HttpClientErrorException.UnprocessableEntity) {
            val errorBody = e.responseBodyAsString
            throw IllegalStateException("토스페이먼츠에 의해 결제 거부됨: $errorBody")
        } catch (e: HttpClientErrorException) {
            throw IllegalStateException("토스페이먼츠 API 오류: ${e.statusCode} - ${e.responseBodyAsString}")
        }
    }

    /**
     * Basic Auth 헤더 생성.
     * 토스페이먼츠는 "secretKey:" 형식을 Base64 인코딩하여 Authorization 헤더에 전달.
     */
    private fun createAuthHeaders(): HttpHeaders {
        val credentials = "$secretKey:"
        val encodedCredentials = Base64.getEncoder().encodeToString(credentials.toByteArray())

        return HttpHeaders().apply {
            set("Authorization", "Basic $encodedCredentials")
            contentType = MediaType.APPLICATION_JSON
        }
    }

    /**
     * 고객 식별 번호 파싱.
     * - 개인: 생년월일 6자리 (YYMMDD)
     * - 법인: 사업자등록번호 10자리
     */
    private fun parseCustomerIdentityNumber(birthDate: String): String {
        return when {
            birthDate.length == 8 -> birthDate.substring(2) // YYYYMMDD → YYMMDD
            birthDate.length == 6 -> birthDate // 이미 YYMMDD
            birthDate.length == 10 -> birthDate // 사업자번호
            else -> birthDate
        }
    }

    /**
     * 주문 ID 생성.
     * 토스페이먼츠 규격: 6~64자, 영문 대소문자, 숫자, -, _
     */
    private fun generateOrderId(): String {
        val uuid = UUID.randomUUID().toString().replace("-", "")
        return "ORDER_${System.currentTimeMillis()}_$uuid".take(64)
    }

    /**
     * 승인 시각 파싱.
     */
    private fun parseApprovedAt(approvedAt: String?): LocalDateTime {
        return if (approvedAt != null) {
            LocalDateTime.parse(approvedAt, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        } else {
            LocalDateTime.now()
        }
    }

    /**
     * 토스페이먼츠 상태를 내부 상태로 매핑.
     */
    private fun mapStatus(status: String): PaymentStatus {
        return when (status) {
            "DONE" -> PaymentStatus.APPROVED
            "CANCELED" -> PaymentStatus.CANCELED
            "PARTIAL_CANCELED" -> PaymentStatus.CANCELED
            "ABORTED" -> PaymentStatus.CANCELED
            "EXPIRED" -> PaymentStatus.CANCELED
            else -> PaymentStatus.APPROVED
        }
    }
}

/**
 * 토스페이먼츠 결제 응답.
 */
data class TossPaymentResponse(
    val paymentKey: String,
    val orderId: String,
    val orderName: String,
    val status: String,
    val approvedAt: String?,
    val card: TossCardInfo?,
    val totalAmount: Int,
    val method: String?,
)

/**
 * 토스페이먼츠 카드 정보.
 */
data class TossCardInfo(
    val issuerCode: String?,
    val acquirerCode: String?,
    val number: String?,
    val cardType: String?,
    val ownerType: String?,
)

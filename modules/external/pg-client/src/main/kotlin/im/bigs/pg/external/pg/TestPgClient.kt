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

/**
 * TestPG API 연동 클라이언트.
 * - partnerId % 3 == 2 인 경우 이 어댑터 사용 (2, 5, 8, 11...)
 * - AES-256-GCM 암호화 사용
 */
@Component
class TestPgClient(
    private val restTemplate: RestTemplate,
    @Value("\${testpg.api-key}") private val apiKey: String,
    @Value("\${testpg.iv}") private val iv: String,
    @Value("\${testpg.base-url}") private val baseUrl: String,
) : PgClientOutPort {

    override fun supports(partnerId: Long): Boolean = partnerId % 3L == 2L

    override fun approve(request: PgApproveRequest): PgApproveResult {
        requireNotNull(request.cardNumber) { "cardNumber는 TestPG에 필수입니다." }
        requireNotNull(request.birthDate) { "TestPG에는 생년월일(birthDate)이 필수입니다." }
        requireNotNull(request.expiry) { "TestPG에는 만료일이 필요합니다" }
        requireNotNull(request.password) { "TestPG에는 비밀번호가 필요합니다." }

        val plaintext = """
            {
                "cardNumber": "${request.cardNumber}",
                "birthDate": "${request.birthDate}",
                "expiry": "${request.expiry}",
                "password": "${request.password}",
                "amount": ${request.amount.toInt()}
            }
        """.trimIndent()

        val encrypted = TestPgEncryptor.encrypt(plaintext, apiKey, iv)

        val headers = HttpHeaders().apply {
            set("API-KEY", apiKey)
            contentType = MediaType.APPLICATION_JSON
        }
        val body = mapOf("enc" to encrypted)

        try {
            val response = restTemplate.exchange(
                "$baseUrl/api/v1/pay/credit-card",
                HttpMethod.POST,
                HttpEntity(body, headers),
                TestPgSuccessResponse::class.java,
            )

            val responseBody = response.body
                ?: throw IllegalStateException("Empty response from TestPG")

            return PgApproveResult(
                approvalCode = responseBody.approvalCode,
                approvedAt = LocalDateTime.parse(
                    responseBody.approvedAt,
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME,
                ),
                maskedCardLast4 = responseBody.maskedCardLast4,
                status = PaymentStatus.APPROVED,
            )
        } catch (e: HttpClientErrorException.UnprocessableEntity) {
            val errorBody = e.responseBodyAsString
            throw IllegalStateException("TestPG에 의해 결제 거부됨: $errorBody")
        } catch (e: HttpClientErrorException.Unauthorized) {
            throw IllegalStateException("TestPG 인증 실패: 잘못된 API 키")
        }
    }
}

data class TestPgSuccessResponse(
    val approvalCode: String,
    val approvedAt: String,
    val maskedCardLast4: String,
    val amount: Int,
    val status: String,
)

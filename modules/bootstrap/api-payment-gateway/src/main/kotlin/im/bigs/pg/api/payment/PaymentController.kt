package im.bigs.pg.api.payment

import im.bigs.pg.api.payment.dto.CreatePaymentRequest
import im.bigs.pg.api.payment.dto.PaymentResponse
import im.bigs.pg.api.payment.dto.QueryResponse
import im.bigs.pg.api.payment.dto.Summary
import im.bigs.pg.application.payment.port.`in`.PaymentCommand
import im.bigs.pg.application.payment.port.`in`.PaymentUseCase
import im.bigs.pg.application.payment.port.`in`.QueryFilter
import im.bigs.pg.application.payment.port.`in`.QueryPaymentsUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

/**
 * 결제 API 진입점.
 * - POST: 결제 생성
 * - GET: 결제 조회(커서 페이지네이션 + 통계)
 */
@RestController
@RequestMapping("/api/v1/payments")
@Validated
@Tag(name = "Payment", description = "결제 관련 API")
class PaymentController(
    private val paymentUseCase: PaymentUseCase,
    private val queryPaymentsUseCase: QueryPaymentsUseCase,
) {

    /**
     * 결제 생성.
     *
     * @param req 결제 요청 본문
     * @return 생성된 결제 요약 응답
     */
    @Operation(
        summary = "결제 생성",
        description = "카드 정보와 금액을 받아 PG사를 통해 결제를 승인하고 결과를 반환합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "결제 승인 성공",
                content = [Content(schema = Schema(implementation = PaymentResponse::class))],
            ),
            ApiResponse(responseCode = "400", description = "잘못된 요청 (필수 필드 누락 등)"),
            ApiResponse(responseCode = "422", description = "PG사 결제 거부"),
        ],
    )
    @PostMapping
    fun create(@RequestBody req: CreatePaymentRequest): ResponseEntity<PaymentResponse> {
        val saved = paymentUseCase.pay(
            PaymentCommand(
                partnerId = req.partnerId,
                amount = req.amount,
                cardNumber = req.cardNumber,
                birthDate = req.birthDate,
                expiry = req.expiry,
                password = req.password,
                cardBin = req.cardBin,
                cardLast4 = req.cardLast4,
                productName = req.productName,
            ),
        )
        return ResponseEntity.ok(PaymentResponse.from(saved))
    }

    /**
     * 결제 조회(커서 기반 페이지네이션 + 통계).
     *
     * @param partnerId 제휴사 필터
     * @param status 상태 필터
     * @param from 조회 시작 시각(ISO-8601)
     * @param to 조회 종료 시각(ISO-8601)
     * @param cursor 다음 페이지 커서
     * @param limit 페이지 크기(기본 20)
     * @return 목록/통계/커서 정보
     */
    @Operation(
        summary = "결제 목록 조회",
        description = "커서 기반 페이지네이션으로 결제 목록을 조회하고, 필터 조건에 맞는 통계 정보를 함께 반환합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "조회 성공",
                content = [Content(schema = Schema(implementation = QueryResponse::class))],
            ),
        ],
    )
    @GetMapping
    fun query(
        @Parameter(description = "제휴사 ID 필터")
        @RequestParam(required = false) partnerId: Long?,
        @Parameter(description = "결제 상태 필터 (APPROVED, REJECTED 등)")
        @RequestParam(required = false) status: String?,
        @Parameter(description = "조회 시작 시각 (yyyy-MM-dd HH:mm:ss)", example = "2025-01-01 00:00:00")
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") from: LocalDateTime?,
        @Parameter(description = "조회 종료 시각 (yyyy-MM-dd HH:mm:ss)", example = "2025-12-31 23:59:59")
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") to: LocalDateTime?,
        @Parameter(description = "다음 페이지 커서 (이전 응답의 nextCursor 값)")
        @RequestParam(required = false) cursor: String?,
        @Parameter(description = "페이지 크기", example = "20")
        @RequestParam(defaultValue = "20") limit: Int,
    ): ResponseEntity<QueryResponse> {
        val res = queryPaymentsUseCase.query(
            QueryFilter(partnerId, status, from, to, cursor, limit),
        )
        return ResponseEntity.ok(
            QueryResponse(
                items = res.items.map { PaymentResponse.from(it) },
                summary = Summary(res.summary.count, res.summary.totalAmount, res.summary.totalNetAmount),
                nextCursor = res.nextCursor,
                hasNext = res.hasNext,
            ),
        )
    }
}

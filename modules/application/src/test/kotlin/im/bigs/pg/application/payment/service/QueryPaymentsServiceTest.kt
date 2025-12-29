package im.bigs.pg.application.payment.service

import im.bigs.pg.application.payment.port.`in`.QueryFilter
import im.bigs.pg.application.payment.port.out.PaymentOutPort
import im.bigs.pg.application.payment.port.out.PaymentPage
import im.bigs.pg.application.payment.port.out.PaymentQuery
import im.bigs.pg.application.payment.port.out.PaymentSummaryFilter
import im.bigs.pg.application.payment.port.out.PaymentSummaryProjection
import im.bigs.pg.domain.payment.Payment
import im.bigs.pg.domain.payment.PaymentStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.DisplayName
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QueryPaymentsServiceTest {
    private val paymentRepository = mockk<PaymentOutPort>()
    private val service = QueryPaymentsService(paymentRepository)

    private fun createPayment(id: Long, createdAt: LocalDateTime) = Payment(
        id = id,
        partnerId = 1L,
        amount = BigDecimal("10000"),
        appliedFeeRate = BigDecimal("0.0300"),
        feeAmount = BigDecimal("300"),
        netAmount = BigDecimal("9700"),
        approvalCode = "APPROVAL-$id",
        approvedAt = createdAt,
        status = PaymentStatus.APPROVED,
        createdAt = createdAt,
        updatedAt = createdAt,
    )

    @Test
    @DisplayName("필터 없이 전체 조회")
    fun `필터 없이 전체 조회`() {
        val payments = listOf(
            createPayment(1L, LocalDateTime.of(2024, 1, 1, 12, 0)),
            createPayment(2L, LocalDateTime.of(2024, 1, 1, 11, 0)),
        )
        every { paymentRepository.findBy(any()) } returns PaymentPage(
            items = payments,
            hasNext = false,
            nextCursorCreatedAt = null,
            nextCursorId = null,
        )
        every { paymentRepository.summary(any()) } returns PaymentSummaryProjection(
            count = 2L,
            totalAmount = BigDecimal("20000"),
            totalNetAmount = BigDecimal("19400"),
        )

        val result = service.query(QueryFilter())

        assertEquals(2, result.items.size)
        assertEquals(2L, result.summary.count)
        assertEquals(BigDecimal("20000"), result.summary.totalAmount)
        assertFalse(result.hasNext)
        assertNull(result.nextCursor)
    }

    @Test
    @DisplayName("partnerId 필터 적용")
    fun `partnerId 필터 적용`() {
        val querySlot = slot<PaymentQuery>()
        val summarySlot = slot<PaymentSummaryFilter>()

        every { paymentRepository.findBy(capture(querySlot)) } returns PaymentPage(
            items = emptyList(),
            hasNext = false,
            nextCursorCreatedAt = null,
            nextCursorId = null,
        )
        every { paymentRepository.summary(capture(summarySlot)) } returns PaymentSummaryProjection(
            count = 0L,
            totalAmount = BigDecimal.ZERO,
            totalNetAmount = BigDecimal.ZERO,
        )

        service.query(QueryFilter(partnerId = 123L))

        assertEquals(123L, querySlot.captured.partnerId)
        assertEquals(123L, summarySlot.captured.partnerId)
    }

    @Test
    @DisplayName("status 필터 적용")
    fun `status 필터 적용`() {
        val querySlot = slot<PaymentQuery>()
        val summarySlot = slot<PaymentSummaryFilter>()

        every { paymentRepository.findBy(capture(querySlot)) } returns PaymentPage(
            items = emptyList(),
            hasNext = false,
            nextCursorCreatedAt = null,
            nextCursorId = null,
        )
        every { paymentRepository.summary(capture(summarySlot)) } returns PaymentSummaryProjection(
            count = 0L,
            totalAmount = BigDecimal.ZERO,
            totalNetAmount = BigDecimal.ZERO,
        )

        service.query(QueryFilter(status = "APPROVED"))

        assertEquals(PaymentStatus.APPROVED, querySlot.captured.status)
        assertEquals(PaymentStatus.APPROVED, summarySlot.captured.status)
    }

    @Test
    @DisplayName("다음 페이지가 있을 때 nextCursor 반환")
    fun `다음 페이지가 있을 때 nextCursor 반환`() {
        val lastCreatedAt = LocalDateTime.of(2024, 1, 1, 12, 0)
        every { paymentRepository.findBy(any()) } returns PaymentPage(
            items = listOf(createPayment(1L, lastCreatedAt)),
            hasNext = true,
            nextCursorCreatedAt = lastCreatedAt,
            nextCursorId = 1L,
        )
        every { paymentRepository.summary(any()) } returns PaymentSummaryProjection(
            count = 5L,
            totalAmount = BigDecimal("50000"),
            totalNetAmount = BigDecimal("48500"),
        )

        val result = service.query(QueryFilter(limit = 1))

        assertTrue(result.hasNext)
        assertNotNull(result.nextCursor)
    }

    @Test
    @DisplayName("커서를 사용해 다음 페이지 조회")
    fun `커서를 사용해 다음 페이지 조회`() {
        val querySlot = slot<PaymentQuery>()

        every { paymentRepository.findBy(capture(querySlot)) } returns PaymentPage(
            items = listOf(createPayment(2L, LocalDateTime.of(2024, 1, 1, 11, 0))),
            hasNext = false,
            nextCursorCreatedAt = null,
            nextCursorId = null,
        )
        every { paymentRepository.summary(any()) } returns PaymentSummaryProjection(
            count = 2L,
            totalAmount = BigDecimal("20000"),
            totalNetAmount = BigDecimal("19400"),
        )

        val cursorTs = LocalDateTime.of(2024, 1, 1, 12, 0).toInstant(java.time.ZoneOffset.UTC).toEpochMilli()
        val cursor = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString("$cursorTs:1".toByteArray())

        val result = service.query(QueryFilter(cursor = cursor))

        assertNotNull(querySlot.captured.cursorCreatedAt)
        assertEquals(1L, querySlot.captured.cursorId)
        assertFalse(result.hasNext)
    }

    @Test
    @DisplayName("기간 필터(from/to) 적용")
    fun `기간 필터 적용`() {
        val querySlot = slot<PaymentQuery>()
        val summarySlot = slot<PaymentSummaryFilter>()
        val from = LocalDateTime.of(2024, 1, 1, 0, 0)
        val to = LocalDateTime.of(2024, 1, 31, 23, 59)

        every { paymentRepository.findBy(capture(querySlot)) } returns PaymentPage(
            items = emptyList(),
            hasNext = false,
            nextCursorCreatedAt = null,
            nextCursorId = null,
        )
        every { paymentRepository.summary(capture(summarySlot)) } returns PaymentSummaryProjection(
            count = 0L,
            totalAmount = BigDecimal.ZERO,
            totalNetAmount = BigDecimal.ZERO,
        )

        service.query(QueryFilter(from = from, to = to))

        assertEquals(from, querySlot.captured.from)
        assertEquals(to, querySlot.captured.to)
        assertEquals(from, summarySlot.captured.from)
        assertEquals(to, summarySlot.captured.to)
    }
}

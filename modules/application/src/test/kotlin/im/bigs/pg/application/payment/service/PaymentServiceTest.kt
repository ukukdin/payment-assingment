package im.bigs.pg.application.payment.service

import im.bigs.pg.application.partner.port.out.FeePolicyOutPort
import im.bigs.pg.application.partner.port.out.PartnerOutPort
import im.bigs.pg.application.payment.port.`in`.PaymentCommand
import im.bigs.pg.application.payment.port.out.PaymentOutPort
import im.bigs.pg.application.pg.port.out.PgApproveRequest
import im.bigs.pg.application.pg.port.out.PgApproveResult
import im.bigs.pg.application.pg.port.out.PgClientOutPort
import im.bigs.pg.domain.partner.FeePolicy
import im.bigs.pg.domain.partner.Partner
import im.bigs.pg.domain.payment.Payment
import im.bigs.pg.domain.payment.PaymentStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PaymentServiceTest {
    private val partnerRepo = mockk<PartnerOutPort>()
    private val feeRepo = mockk<FeePolicyOutPort>()
    private val paymentRepo = mockk<PaymentOutPort>()
    private val pgClient = object : PgClientOutPort {
        override fun supports(partnerId: Long) = true
        override fun approve(request: PgApproveRequest) =
            PgApproveResult(
                approvalCode = "APPROVAL-123",
                approvedAt = LocalDateTime.of(2024, 1, 1, 0, 0),
                maskedCardLast4 = request.cardNumber?.replace("-", "")?.takeLast(4) ?: request.cardLast4,
                status = PaymentStatus.APPROVED,
            )
    }

    private fun createService() = PaymentService(partnerRepo, feeRepo, paymentRepo, listOf(pgClient))

    @Test
    @DisplayName("결제 시 수수료 정책을 적용하고 저장해야 한다")
    fun `결제 시 수수료 정책을 적용하고 저장해야 한다`() {
        val service = createService()
        every { partnerRepo.findById(1L) } returns Partner(1L, "TEST", "Test", true)
        every { feeRepo.findEffectivePolicy(1L, any()) } returns FeePolicy(
            id = 10L,
            partnerId = 1L,
            effectiveFrom = LocalDateTime.ofInstant(Instant.parse("2020-01-01T00:00:00Z"), ZoneOffset.UTC),
            percentage = BigDecimal("0.0300"),
            fixedFee = BigDecimal("100"),
        )
        val savedSlot = slot<Payment>()
        every { paymentRepo.save(capture(savedSlot)) } answers { savedSlot.captured.copy(id = 99L) }

        val cmd = PaymentCommand(partnerId = 1L, amount = BigDecimal("10000"), cardLast4 = "4242")
        val res = service.pay(cmd)

        assertEquals(99L, res.id)
        assertEquals(BigDecimal("400"), res.feeAmount)
        assertEquals(BigDecimal("9600"), res.netAmount)
        assertEquals(PaymentStatus.APPROVED, res.status)
    }

    @Test
    @DisplayName("존재하지 않는 제휴사로 결제 시 예외 발생")
    fun `존재하지 않는 제휴사로 결제 시 예외 발생`() {
        val service = createService()
        every { partnerRepo.findById(999L) } returns null

        val cmd = PaymentCommand(partnerId = 999L, amount = BigDecimal("10000"))

        val exception = assertThrows<IllegalArgumentException> { service.pay(cmd) }
        assertTrue(exception.message!!.contains("Partner not found"))
    }

    @Test
    @DisplayName("비활성 제휴사로 결제 시 예외 발생")
    fun `비활성 제휴사로 결제 시 예외 발생`() {
        val service = createService()
        every { partnerRepo.findById(1L) } returns Partner(1L, "TEST", "Test", false)

        val cmd = PaymentCommand(partnerId = 1L, amount = BigDecimal("10000"))

        val exception = assertThrows<IllegalArgumentException> { service.pay(cmd) }
        assertTrue(exception.message!!.contains("inactive"))
    }

    @Test
    @DisplayName("수수료 정책 없을 시 예외 발생")
    fun `수수료 정책 없을 시 예외 발생`() {
        val service = createService()
        every { partnerRepo.findById(1L) } returns Partner(1L, "TEST", "Test", true)
        every { feeRepo.findEffectivePolicy(1L, any()) } returns null

        val cmd = PaymentCommand(partnerId = 1L, amount = BigDecimal("10000"))

        val exception = assertThrows<IllegalStateException> { service.pay(cmd) }
        assertTrue(exception.message!!.contains("No fee policy"))
    }

    @Test
    @DisplayName("고정 수수료 없이 비율만 적용")
    fun `고정 수수료 없이 비율만 적용`() {
        val service = createService()
        every { partnerRepo.findById(1L) } returns Partner(1L, "TEST", "Test", true)
        every { feeRepo.findEffectivePolicy(1L, any()) } returns FeePolicy(
            id = 10L,
            partnerId = 1L,
            effectiveFrom = LocalDateTime.ofInstant(Instant.parse("2020-01-01T00:00:00Z"), ZoneOffset.UTC),
            percentage = BigDecimal("0.0250"),
            fixedFee = null,
        )
        val savedSlot = slot<Payment>()
        every { paymentRepo.save(capture(savedSlot)) } answers { savedSlot.captured.copy(id = 99L) }

        val cmd = PaymentCommand(partnerId = 1L, amount = BigDecimal("10000"))
        val res = service.pay(cmd)

        assertEquals(BigDecimal("250"), res.feeAmount)
        assertEquals(BigDecimal("9750"), res.netAmount)
    }
}

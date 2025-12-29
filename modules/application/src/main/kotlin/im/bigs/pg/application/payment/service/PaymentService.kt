package im.bigs.pg.application.payment.service

import im.bigs.pg.application.partner.port.out.FeePolicyOutPort
import im.bigs.pg.application.partner.port.out.PartnerOutPort
import im.bigs.pg.application.payment.port.`in`.PaymentCommand
import im.bigs.pg.application.payment.port.`in`.PaymentUseCase
import im.bigs.pg.application.payment.port.out.PaymentOutPort
import im.bigs.pg.application.pg.port.out.PgApproveRequest
import im.bigs.pg.application.pg.port.out.PgClientOutPort
import im.bigs.pg.domain.calculation.FeeCalculator
import im.bigs.pg.domain.payment.Payment
import im.bigs.pg.domain.payment.PaymentStatus
import org.springframework.stereotype.Service

/**
 * 결제 생성 유스케이스 구현체.
 * - 입력(REST 등) → 도메인/외부PG/영속성 포트를 순차적으로 호출하는 흐름을 담당합니다.
 * - 수수료 정책 조회 및 적용(계산)은 도메인 유틸리티를 통해 수행합니다.
 */
@Service
class PaymentService(
    private val partnerRepository: PartnerOutPort,
    private val feePolicyRepository: FeePolicyOutPort,
    private val paymentRepository: PaymentOutPort,
    private val pgClients: List<PgClientOutPort>,
) : PaymentUseCase {
    /**
     * 결제 승인/수수료 계산/저장을 순차적으로 수행합니다.
     * - 현재 예시 구현은 하드코드된 수수료(3% + 100)로 계산합니다.
     * - 과제: 제휴사별 수수료 정책을 적용하도록 개선해 보세요.
     */
    override fun pay(command: PaymentCommand): Payment {
        val partner =
            partnerRepository.findById(command.partnerId)
                ?: throw IllegalArgumentException("Partner not found: ${command.partnerId}")
        require(partner.active) { "Partner is inactive: ${partner.id}" }

        val pgClient =
            pgClients.firstOrNull { it.supports(partner.id) }
                ?: throw IllegalStateException("No PG client for partner ${partner.id}")

        val approve =
            pgClient.approve(
                PgApproveRequest(
                    partnerId = partner.id,
                    amount = command.amount,
                    cardNumber = command.cardNumber,
                    birthDate = command.birthDate,
                    expiry = command.expiry,
                    password = command.password,
                    cardBin = command.cardBin,
                    cardLast4 = command.cardLast4,
                    productName = command.productName,
                ),
            )
        val policy = feePolicyRepository.findEffectivePolicy(partner.id)
            ?: throw IllegalStateException("No fee policy for partner ${partner.id}")
        val (fee, net) = FeeCalculator.calculateFee(command.amount, policy.percentage, policy.fixedFee)

        // cardBin: cardNumber에서 앞 6자리 추출, 없으면 command.cardBin 사용
        val cardBin = command.cardNumber?.replace("-", "")?.take(6) ?: command.cardBin
        // cardLast4: PG 응답의 maskedCardLast4 우선 사용
        val cardLast4 = approve.maskedCardLast4 ?: command.cardLast4

        val payment =
            Payment(
                partnerId = partner.id,
                amount = command.amount,
                appliedFeeRate = policy.percentage,
                feeAmount = fee,
                netAmount = net,
                cardBin = cardBin,
                cardLast4 = cardLast4,
                approvalCode = approve.approvalCode,
                approvedAt = approve.approvedAt,
                status = PaymentStatus.APPROVED,
            )

        return paymentRepository.save(payment)
    }
}

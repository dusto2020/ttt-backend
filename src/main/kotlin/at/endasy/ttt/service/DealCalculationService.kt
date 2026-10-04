package at.endasy.ttt.service

import java.math.BigDecimal
import java.math.RoundingMode
import org.springframework.stereotype.Service

enum class DealTier(val label: String, val color: String) {
    TOP_DEAL("Hervorragend", "#00F076"),
    STRONG("Sehr lohnenswert", "#00C853"),
    SOLID("Solide", "#FFB300"),
    LOW_MARGIN("Geringe Marge", "#FF8A00"),
    UNPROFITABLE("Unrentabel", "#94A3B8"),
}

data class PaypalCashbackResult(
    val netCashProfit: BigDecimal,
)

data class TemuCreditResult(
    val effectiveOutlay: BigDecimal,
    val floatRatio: BigDecimal?,
)

@Service
class DealCalculationService {

    fun cardmarketNet(effectiveCmPrice: BigDecimal): BigDecimal =
        effectiveCmPrice.multiply(BigDecimal.ONE.subtract(CARDMARKET_FEE_PERCENT)).setScale(2, RoundingMode.HALF_UP)

    fun markupPercent(temuPrice: BigDecimal, effectiveCmPrice: BigDecimal): BigDecimal? {
        if (effectiveCmPrice.signum() == 0) return null
        return temuPrice.subtract(effectiveCmPrice)
            .divide(effectiveCmPrice, 6, RoundingMode.HALF_UP)
            .multiply(BigDecimal(100))
            .setScale(2, RoundingMode.HALF_UP)
    }

    fun dealTier(markupPct: BigDecimal): DealTier = when {
        markupPct < BigDecimal(15) -> DealTier.TOP_DEAL
        markupPct < BigDecimal(28) -> DealTier.STRONG
        markupPct < BigDecimal(42) -> DealTier.SOLID
        markupPct < BigDecimal(55) -> DealTier.LOW_MARGIN
        else -> DealTier.UNPROFITABLE
    }

    fun paypalCashbackScenario(
        cmNet: BigDecimal,
        cashbackAmount: BigDecimal,
        temuPrice: BigDecimal,
    ): PaypalCashbackResult =
        PaypalCashbackResult(
            netCashProfit = cmNet.add(cashbackAmount).subtract(temuPrice).setScale(2, RoundingMode.HALF_UP),
        )

    fun temuCreditScenario(
        cmNet: BigDecimal,
        cashbackAmount: BigDecimal,
        temuPrice: BigDecimal,
    ): TemuCreditResult {
        val effectiveOutlay = temuPrice.subtract(cmNet).setScale(2, RoundingMode.HALF_UP)
        val floatRatio = if (effectiveOutlay.signum() != 0) {
            cashbackAmount.divide(effectiveOutlay, 4, RoundingMode.HALF_UP)
        } else {
            null
        }
        return TemuCreditResult(effectiveOutlay = effectiveOutlay, floatRatio = floatRatio)
    }

    companion object {
        val CARDMARKET_FEE_PERCENT: BigDecimal = BigDecimal("0.05")
    }
}

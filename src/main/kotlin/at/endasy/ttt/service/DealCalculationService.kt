package at.endasy.ttt.service

import java.math.BigDecimal
import java.math.RoundingMode
import org.springframework.stereotype.Service

enum class DealTier(val label: String, val color: String) {
    GOD_TIER("🔥 GOD TIER", "#00F076"),
    EXCELLENT("✅ EXCELLENT", "#00C853"),
    DECENT("⚠️ DECENT", "#FFB300"),
    MEDIOCRE("🛑 MEDIOCRE", "#FF8A00"),
    TRASH("💀 TRASH", "#FF3B57"),
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
        markupPct < BigDecimal(15) -> DealTier.GOD_TIER
        markupPct < BigDecimal(28) -> DealTier.EXCELLENT
        markupPct < BigDecimal(42) -> DealTier.DECENT
        markupPct < BigDecimal(55) -> DealTier.MEDIOCRE
        else -> DealTier.TRASH
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

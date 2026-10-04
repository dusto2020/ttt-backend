package at.endasy.ttt.service

import at.endasy.ttt.dto.DealDto
import at.endasy.ttt.model.CountryCode
import at.endasy.ttt.model.LanguageCode
import com.teamrestocks.ttt.jooq.tables.references.PRODUCTS
import java.math.RoundingMode
import org.jooq.DSLContext
import org.springframework.stereotype.Service

@Service
class DealService(
    private val dsl: DSLContext,
    private val dealCalculationService: DealCalculationService,
) {

    fun findActiveDeals(countryCode: CountryCode? = null): List<DealDto> =
        dsl.selectFrom(PRODUCTS)
            .where(PRODUCTS.IS_ACTIVE.isTrue)
            .apply { countryCode?.let { and(PRODUCTS.COUNTRY_CODE.eq(it.wireName)) } }
            .orderBy(PRODUCTS.NAME)
            .fetch()
            .map { product ->
                val spreadRatio = if (product.temuPrice != null && product.temuPrice!!.signum() != 0) {
                    product.cardmarketPrice!!.divide(product.temuPrice, 4, RoundingMode.HALF_UP)
                } else {
                    null
                }

                val effectiveCmPrice = product.manualPriceOverride
                    ?: product.cardmarketLowestPrice
                    ?: product.cardmarketPrice
                val cardmarketNetPrice = effectiveCmPrice?.let { dealCalculationService.cardmarketNet(it) }
                val markupPercent = effectiveCmPrice?.let {
                    dealCalculationService.markupPercent(product.temuPrice!!, it)
                }
                val dealTier = markupPercent?.let { dealCalculationService.dealTier(it) }

                DealDto(
                    id = product.id!!,
                    name = product.name!!,
                    languageCode = LanguageCode.entries.find { it.wireName == product.languageCode } ?: LanguageCode.EN,
                    countryCode = CountryCode.entries.find { it.wireName == product.countryCode } ?: CountryCode.DE,
                    temuAffiliateUrl = product.temuAffiliateUrl!!,
                    cardmarketUrl = product.cardmarketUrl!!,
                    temuPrice = product.temuPrice!!,
                    cardmarketPrice = product.cardmarketPrice!!,
                    spreadRatio = spreadRatio,
                    cardmarketLowestPrice = product.cardmarketLowestPrice,
                    cardmarketNetPrice = cardmarketNetPrice,
                    markupPercent = markupPercent,
                    dealTier = dealTier?.name,
                    dealTierLabel = dealTier?.label,
                    dealTierColor = dealTier?.color,
                    cardmarketUpdatedAt = product.cardmarketUpdatedAt?.toInstant(),
                    manualPriceOverride = product.manualPriceOverride,
                )
            }
}

package at.endasy.ttt.service

import at.endasy.ttt.dto.DealDto
import at.endasy.ttt.model.CountryCode
import at.endasy.ttt.model.LanguageCode
import com.teamrestocks.ttt.jooq.tables.references.PRODUCTS
import com.teamrestocks.ttt.jooq.tables.references.SELLERS
import java.math.RoundingMode
import java.util.UUID
import org.jooq.DSLContext
import org.springframework.stereotype.Service

@Service
class DealService(
    private val dsl: DSLContext,
    private val dealCalculationService: DealCalculationService,
) {

    fun findActiveDeals(countryCode: CountryCode? = null): List<DealDto> {
        val conditions = mutableListOf(PRODUCTS.IS_ACTIVE.isTrue)
        if (countryCode != null) {
            conditions.add(PRODUCTS.COUNTRY_CODE.eq(countryCode.wireName))
        }

        return dsl.select(
            PRODUCTS.ID,
            PRODUCTS.NAME,
            PRODUCTS.LANGUAGE_CODE,
            PRODUCTS.COUNTRY_CODE,
            PRODUCTS.TEMU_AFFILIATE_URL,
            PRODUCTS.CARDMARKET_URL,
            PRODUCTS.TEMU_PRICE,
            PRODUCTS.CARDMARKET_LOWEST_PRICE,
            PRODUCTS.CARDMARKET_UPDATED_AT,
            PRODUCTS.MANUAL_PRICE_OVERRIDE,
            PRODUCTS.SELLER_ID,
            SELLERS.NAME.`as`("seller_name"),
        )
            .from(PRODUCTS)
            .leftJoin(SELLERS).on(PRODUCTS.SELLER_ID.eq(SELLERS.ID))
            .where(conditions)
            .orderBy(PRODUCTS.NAME)
            .fetch()
            .map { r ->
                val temuPrice = r.get(PRODUCTS.TEMU_PRICE)!!
                val manualOverride = r.get(PRODUCTS.MANUAL_PRICE_OVERRIDE)
                val cmLowestPrice = r.get(PRODUCTS.CARDMARKET_LOWEST_PRICE)

                val effectiveCmPrice = manualOverride ?: cmLowestPrice

                val spreadRatio = if (effectiveCmPrice != null && temuPrice.signum() != 0) {
                    effectiveCmPrice.divide(temuPrice, 4, RoundingMode.HALF_UP)
                } else null

                val cardmarketNetPrice = effectiveCmPrice?.let { dealCalculationService.cardmarketNet(it) }
                val markupPercent = effectiveCmPrice?.let {
                    dealCalculationService.markupPercent(temuPrice, it)
                }
                val dealTier = markupPercent?.let { dealCalculationService.dealTier(it) }

                val langWire = r.get(PRODUCTS.LANGUAGE_CODE)
                val countryWire = r.get(PRODUCTS.COUNTRY_CODE)

                DealDto(
                    id = r.get(PRODUCTS.ID)!!,
                    name = r.get(PRODUCTS.NAME)!!,
                    languageCode = LanguageCode.entries.find { it.wireName == langWire } ?: LanguageCode.EN,
                    countryCode = CountryCode.entries.find { it.wireName == countryWire } ?: CountryCode.DE,
                    temuAffiliateUrl = r.get(PRODUCTS.TEMU_AFFILIATE_URL)!!,
                    cardmarketUrl = r.get(PRODUCTS.CARDMARKET_URL)!!,
                    temuPrice = temuPrice,
                    spreadRatio = spreadRatio,
                    cardmarketLowestPrice = cmLowestPrice,
                    cardmarketNetPrice = cardmarketNetPrice,
                    markupPercent = markupPercent,
                    dealTier = dealTier?.name,
                    dealTierLabel = dealTier?.label,
                    dealTierColor = dealTier?.color,
                    cardmarketUpdatedAt = r.get(PRODUCTS.CARDMARKET_UPDATED_AT)?.toInstant(),
                    manualPriceOverride = manualOverride,
                    sellerId = r.get(PRODUCTS.SELLER_ID),
                    sellerName = r.get("seller_name", String::class.java),
                )
            }
    }
}
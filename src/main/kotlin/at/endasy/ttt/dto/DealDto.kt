package at.endasy.ttt.dto

import at.endasy.ttt.model.CountryCode
import at.endasy.ttt.model.LanguageCode
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class DealDto(
    val id: UUID,
    val name: String,
    val languageCode: LanguageCode,
    val countryCode: CountryCode,
    val temuAffiliateUrl: String,
    val cardmarketUrl: String,
    val temuPrice: BigDecimal,
    val spreadRatio: BigDecimal?,
    val cardmarketLowestPrice: BigDecimal?,
    val cardmarketNetPrice: BigDecimal?,
    val markupPercent: BigDecimal?,
    val dealTier: String?,
    val dealTierLabel: String?,
    val dealTierColor: String?,
    val cardmarketUpdatedAt: Instant?,
    val manualPriceOverride: BigDecimal?,
)

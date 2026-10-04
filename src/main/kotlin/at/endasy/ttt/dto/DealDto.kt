package at.endasy.ttt.dto

import java.math.BigDecimal
import java.util.UUID

data class DealDto(
    val id: UUID,
    val name: String,
    val languageCode: String,
    val temuAffiliateUrl: String,
    val cardmarketUrl: String,
    val temuPrice: BigDecimal,
    val cardmarketPrice: BigDecimal,
    val spreadRatio: BigDecimal?,
)

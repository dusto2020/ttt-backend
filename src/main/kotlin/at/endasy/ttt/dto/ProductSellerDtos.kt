package at.endasy.ttt.dto

import at.endasy.ttt.model.CountryCode
import at.endasy.ttt.model.LanguageCode
import com.teamrestocks.ttt.jooq.tables.records.ProductsRecord
import com.teamrestocks.ttt.jooq.tables.records.SellersRecord
import com.teamrestocks.ttt.jooq.tables.records.UserDevicesRecord
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class ProductRequest(
    val name: String,
    val languageCode: LanguageCode = LanguageCode.EN,
    val countryCode: CountryCode = CountryCode.DE,
    val temuAffiliateUrl: String,
    val cardmarketUrl: String,
    val temuPrice: BigDecimal,
    val isActive: Boolean = true,
    val manualPriceOverride: BigDecimal? = null,
    val temuProductUrl: String? = null,
)

data class SellerRequest(
    val name: String,
    val storeUrl: String?,
    val countryCode: CountryCode = CountryCode.DE,
    val isActive: Boolean = true,
)

data class DeviceRequest(
    val label: String,
)

data class ProductResponse(
    val id: UUID,
    val name: String,
    val languageCode: LanguageCode,
    val countryCode: CountryCode,
    val temuAffiliateUrl: String,
    val cardmarketUrl: String,
    val temuPrice: BigDecimal,
    val isActive: Boolean,
    val updatedAt: OffsetDateTime?,
    val cardmarketLowestPrice: BigDecimal?,
    val cardmarketUpdatedAt: OffsetDateTime?,
    val manualPriceOverride: BigDecimal?,
    val temuProductUrl: String?,
)

data class SellerResponse(
    val id: UUID,
    val name: String,
    val storeUrl: String?,
    val countryCode: CountryCode,
    val isActive: Boolean,
)

data class DeviceResponse(
    val id: UUID,
    val userId: UUID,
    val label: String,
    val createdAt: OffsetDateTime?,
)

fun ProductsRecord.toResponse() = ProductResponse(
    id = id!!,
    name = name!!,
    languageCode = LanguageCode.entries.find { it.wireName == languageCode } ?: LanguageCode.EN,
    countryCode = CountryCode.entries.find { it.wireName == countryCode } ?: CountryCode.DE,
    temuAffiliateUrl = temuAffiliateUrl!!,
    cardmarketUrl = cardmarketUrl!!,
    temuPrice = temuPrice!!,
    isActive = isActive ?: false,
    updatedAt = updatedAt,
    cardmarketLowestPrice = cardmarketLowestPrice,
    cardmarketUpdatedAt = cardmarketUpdatedAt,
    manualPriceOverride = manualPriceOverride,
    temuProductUrl = temuProductUrl,
)

fun SellersRecord.toResponse() = SellerResponse(
    id = id!!,
    name = name!!,
    storeUrl = storeUrl,
    countryCode = CountryCode.entries.find { it.wireName == countryCode } ?: CountryCode.DE,
    isActive = isActive ?: false,
)

fun UserDevicesRecord.toResponse() = DeviceResponse(
    id = id!!,
    userId = userId!!,
    label = label!!,
    createdAt = createdAt,
)

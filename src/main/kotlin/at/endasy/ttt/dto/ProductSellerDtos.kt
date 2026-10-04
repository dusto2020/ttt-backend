package at.endasy.ttt.dto

import com.teamrestocks.ttt.jooq.tables.records.ProductsRecord
import com.teamrestocks.ttt.jooq.tables.records.SellersRecord
import com.teamrestocks.ttt.jooq.tables.records.UserDevicesRecord
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class ProductRequest(
    val name: String,
    val languageCode: String = "EN",
    val temuAffiliateUrl: String,
    val cardmarketUrl: String,
    val temuPrice: BigDecimal,
    val cardmarketPrice: BigDecimal,
    val isActive: Boolean = true,
)

data class SellerRequest(
    val name: String,
    val storeUrl: String? = null,
    val isActive: Boolean = true,
)

data class DeviceRequest(
    val label: String,
)

data class ProductResponse(
    val id: UUID,
    val name: String,
    val languageCode: String,
    val temuAffiliateUrl: String,
    val cardmarketUrl: String,
    val temuPrice: BigDecimal,
    val cardmarketPrice: BigDecimal,
    val isActive: Boolean,
    val updatedAt: OffsetDateTime?,
)

data class SellerResponse(
    val id: UUID,
    val name: String,
    val storeUrl: String?,
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
    languageCode = languageCode!!,
    temuAffiliateUrl = temuAffiliateUrl!!,
    cardmarketUrl = cardmarketUrl!!,
    temuPrice = temuPrice!!,
    cardmarketPrice = cardmarketPrice!!,
    isActive = isActive ?: false,
    updatedAt = updatedAt,
)

fun SellersRecord.toResponse() = SellerResponse(
    id = id!!,
    name = name!!,
    storeUrl = storeUrl,
    isActive = isActive ?: false,
)

fun UserDevicesRecord.toResponse() = DeviceResponse(
    id = id!!,
    userId = userId!!,
    label = label!!,
    createdAt = createdAt,
)

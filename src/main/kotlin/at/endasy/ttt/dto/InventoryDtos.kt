package at.endasy.ttt.dto

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class InventoryItemResponse(
    val id: UUID,
    val orderId: UUID,
    val orderSn: String,
    val productId: UUID?,
    val productName: String,
    val languageCode: String?,
    val countryCode: String,
    val isFiller: Boolean,
    val fillerCost: BigDecimal?,
    val status: String, // IN_STOCK, SOLD, KEPT
    val liveCardmarketPrice: BigDecimal?,
    val marketPriceSnapshot: BigDecimal?,
    val resalePrice: BigDecimal?,
    val resalePlatform: String?,
    val resoldAt: Instant?,
    val orderDate: Instant?,
)

data class InventorySummaryResponse(
    val inStockCount: Int,
    val inStockMarketValue: BigDecimal,
    val soldCount: Int,
    val totalRealizedResales: BigDecimal,
    val keptCount: Int,
    val totalKeptSnapshotValue: BigDecimal,
)

data class MarkSoldRequest(
    val resalePrice: BigDecimal,
    val platform: String? = "Cardmarket",
)
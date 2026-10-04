package at.endasy.ttt.service

import at.endasy.ttt.dto.InventoryItemResponse
import at.endasy.ttt.dto.InventorySummaryResponse
import com.teamrestocks.ttt.jooq.tables.references.ORDERS
import com.teamrestocks.ttt.jooq.tables.references.ORDER_ITEMS
import com.teamrestocks.ttt.jooq.tables.references.PRODUCTS
import java.math.BigDecimal
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import org.jooq.DSLContext
import org.springframework.stereotype.Service

@Service
class InventoryService(private val dsl: DSLContext) {

    fun listInventoryForUser(userId: UUID, statusFilter: String? = null): List<InventoryItemResponse> {
        val query = dsl.select(
            ORDER_ITEMS.ID,
            ORDER_ITEMS.ORDER_ID,
            ORDERS.ORDER_SN,
            ORDERS.COUNTRY_CODE,
            ORDERS.CREATED_AT,
            ORDER_ITEMS.PRODUCT_ID,
            PRODUCTS.NAME,
            PRODUCTS.LANGUAGE_CODE,
            PRODUCTS.CARDMARKET_LOWEST_PRICE,
            PRODUCTS.MANUAL_PRICE_OVERRIDE,
            ORDER_ITEMS.IS_FILLER,
            ORDER_ITEMS.CUSTOM_NAME,
            ORDER_ITEMS.FILLER_COST,
            ORDER_ITEMS.STATUS,
            ORDER_ITEMS.MARKET_PRICE_SNAPSHOT,
            ORDER_ITEMS.RESALE_PRICE,
            ORDER_ITEMS.RESALE_PLATFORM,
            ORDER_ITEMS.RESOLD_AT,
        )
            .from(ORDER_ITEMS)
            .join(ORDERS).on(ORDER_ITEMS.ORDER_ID.eq(ORDERS.ID))
            .leftJoin(PRODUCTS).on(ORDER_ITEMS.PRODUCT_ID.eq(PRODUCTS.ID))
            .where(ORDERS.USER_ID.eq(userId))

        if (!statusFilter.isNullOrBlank() && statusFilter != "ALL") {
            query.and(ORDER_ITEMS.STATUS.eq(statusFilter))
        }

        return query.orderBy(ORDERS.CREATED_AT.desc(), ORDER_ITEMS.ID.desc())
            .fetch { r ->
                val isFiller = r.get(ORDER_ITEMS.IS_FILLER) ?: false
                val customName = r.get(ORDER_ITEMS.CUSTOM_NAME)
                val prodName = r.get(PRODUCTS.NAME) ?: "Unbekannt"
                val displayName = if (isFiller) (customName ?: "Füllartikel") else prodName

                val liveCmPrice = if (!isFiller) {
                    r.get(PRODUCTS.MANUAL_PRICE_OVERRIDE) ?: r.get(PRODUCTS.CARDMARKET_LOWEST_PRICE)
                } else null

                InventoryItemResponse(
                    id = r.get(ORDER_ITEMS.ID)!!,
                    orderId = r.get(ORDER_ITEMS.ORDER_ID)!!,
                    orderSn = r.get(ORDERS.ORDER_SN)!!,
                    productId = r.get(ORDER_ITEMS.PRODUCT_ID),
                    productName = displayName,
                    languageCode = r.get(PRODUCTS.LANGUAGE_CODE),
                    countryCode = r.get(ORDERS.COUNTRY_CODE) ?: "DE",
                    isFiller = isFiller,
                    fillerCost = r.get(ORDER_ITEMS.FILLER_COST),
                    status = r.get(ORDER_ITEMS.STATUS) ?: "IN_STOCK",
                    liveCardmarketPrice = liveCmPrice,
                    marketPriceSnapshot = r.get(ORDER_ITEMS.MARKET_PRICE_SNAPSHOT),
                    resalePrice = r.get(ORDER_ITEMS.RESALE_PRICE),
                    resalePlatform = r.get(ORDER_ITEMS.RESALE_PLATFORM),
                    resoldAt = r.get(ORDER_ITEMS.RESOLD_AT)?.toInstant(),
                    orderDate = r.get(ORDERS.CREATED_AT)?.toInstant(),
                )
            }
    }

    fun getSummaryForUser(userId: UUID): InventorySummaryResponse {
        val items = listInventoryForUser(userId, "ALL")

        val inStock = items.filter { it.status == "IN_STOCK" }
        val sold = items.filter { it.status == "SOLD" }
        val kept = items.filter { it.status == "KEPT" }

        val inStockMarketValue = inStock.mapNotNull { it.liveCardmarketPrice }.fold(BigDecimal.ZERO, BigDecimal::add)
        val totalResales = sold.mapNotNull { it.resalePrice }.fold(BigDecimal.ZERO, BigDecimal::add)
        val totalKeptSnapshot = kept.mapNotNull { it.marketPriceSnapshot }.fold(BigDecimal.ZERO, BigDecimal::add)

        return InventorySummaryResponse(
            inStockCount = inStock.size,
            inStockMarketValue = inStockMarketValue,
            soldCount = sold.size,
            totalRealizedResales = totalResales,
            keptCount = kept.size,
            totalKeptSnapshotValue = totalKeptSnapshot,
        )
    }

    /**
     * Als verkauft markieren: Erlös eintragen & aktuellen CM-Preis als historischen Snapshot festhalten
     */
    fun markAsSold(userId: UUID, itemId: UUID, resalePrice: BigDecimal, platform: String?): Boolean {
        val item = findItemForUser(userId, itemId) ?: return false
        val currentMarketPrice = item.get(PRODUCTS.CARDMARKET_LOWEST_PRICE)

        val updated = dsl.update(ORDER_ITEMS)
            .set(ORDER_ITEMS.STATUS, "SOLD")
            .set(ORDER_ITEMS.RESALE_PRICE, resalePrice)
            .set(ORDER_ITEMS.RESALE_PLATFORM, platform ?: "Cardmarket")
            .set(ORDER_ITEMS.RESOLD_AT, OffsetDateTime.ofInstant(Instant.now(), ZoneOffset.UTC))
            .set(ORDER_ITEMS.MARKET_PRICE_SNAPSHOT, currentMarketPrice)
            .where(ORDER_ITEMS.ID.eq(itemId))
            .execute()

        return updated > 0
    }

    /**
     * Für eigene Sammlung behalten (Eigenbedarf):
     * Friert den aktuellen Cardmarket-Preis genau in dieser Sekunde als unveränderlichen Snapshot ein!
     */
    fun markAsKept(userId: UUID, itemId: UUID): Boolean {
        val item = findItemForUser(userId, itemId) ?: return false
        val currentMarketPrice = item.get(PRODUCTS.MANUAL_PRICE_OVERRIDE) ?: item.get(PRODUCTS.CARDMARKET_LOWEST_PRICE)

        val updated = dsl.update(ORDER_ITEMS)
            .set(ORDER_ITEMS.STATUS, "KEPT")
            .set(ORDER_ITEMS.MARKET_PRICE_SNAPSHOT, currentMarketPrice)
            .where(ORDER_ITEMS.ID.eq(itemId))
            .execute()

        return updated > 0
    }

    /**
     * Zurück auf "Im Bestand" setzen (Undo)
     */
    fun markAsInStock(userId: UUID, itemId: UUID): Boolean {
        findItemForUser(userId, itemId) ?: return false

        val updated = dsl.update(ORDER_ITEMS)
            .set(ORDER_ITEMS.STATUS, "IN_STOCK")
            .setNull(ORDER_ITEMS.RESALE_PRICE)
            .setNull(ORDER_ITEMS.RESALE_PLATFORM)
            .setNull(ORDER_ITEMS.RESOLD_AT)
            .setNull(ORDER_ITEMS.MARKET_PRICE_SNAPSHOT)
            .where(ORDER_ITEMS.ID.eq(itemId))
            .execute()

        return updated > 0
    }

    private fun findItemForUser(userId: UUID, itemId: UUID) =
        dsl.select(ORDER_ITEMS.ID, PRODUCTS.CARDMARKET_LOWEST_PRICE, PRODUCTS.MANUAL_PRICE_OVERRIDE)
            .from(ORDER_ITEMS)
            .join(ORDERS).on(ORDER_ITEMS.ORDER_ID.eq(ORDERS.ID))
            .leftJoin(PRODUCTS).on(ORDER_ITEMS.PRODUCT_ID.eq(PRODUCTS.ID))
            .where(ORDER_ITEMS.ID.eq(itemId).and(ORDERS.USER_ID.eq(userId)))
            .fetchOne()
}
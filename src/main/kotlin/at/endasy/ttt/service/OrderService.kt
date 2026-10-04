package at.endasy.ttt.service

import at.endasy.ttt.dto.CreateOrderRequest
import at.endasy.ttt.dto.OrderItemRequest
import at.endasy.ttt.dto.OrderItemResponse
import at.endasy.ttt.dto.OrderResponse
import at.endasy.ttt.model.CountryCode
import at.endasy.ttt.model.EventType
import at.endasy.ttt.model.RewardType
import com.teamrestocks.ttt.jooq.tables.records.OrdersRecord
import com.teamrestocks.ttt.jooq.tables.references.ORDERS
import com.teamrestocks.ttt.jooq.tables.references.ORDER_ITEMS
import java.util.UUID
import org.jooq.DSLContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private fun OrdersRecord.toOrderResponse(items: List<OrderItemResponse>): OrderResponse {
    val id = checkNotNull(id) { "Order id must not be null" }
    val userId = checkNotNull(userId) { "Order $id has no user_id" }
    val orderSn = checkNotNull(orderSn) { "Order $id has no order_sn" }
    val eventTypeWire = checkNotNull(eventType) { "Order $id has no event_type" }
    val rewardTypeWire = checkNotNull(rewardType) { "Order $id has no reward_type" }
    val countryCodeWire = checkNotNull(countryCode) { "Order $id has no country_code" }
    val eventType = checkNotNull(EventType.entries.find { it.wireName == eventTypeWire }) {
        "Order $id has unknown event_type '$eventTypeWire'"
    }
    val rewardType = checkNotNull(RewardType.entries.find { it.wireName == rewardTypeWire }) {
        "Order $id has unknown reward_type '$rewardTypeWire'"
    }
    val countryCode = checkNotNull(CountryCode.entries.find { it.wireName == countryCodeWire }) {
        "Order $id has unknown country_code '$countryCodeWire'"
    }
    val cashPaid = checkNotNull(cashPaid) { "Order $id has no cash_paid" }
    val creditUsed = checkNotNull(creditUsed) { "Order $id has no credit_used" }
    val totalReturn = checkNotNull(totalReturn) { "Order $id has no total_return" }
    val claimedAmount = checkNotNull(claimedAmount) { "Order $id has no claimed_amount" }

    return OrderResponse(
        id = id,
        userId = userId,
        deviceId = deviceId,
        sellerId = sellerId,
        orderSn = orderSn,
        eventType = eventType,
        rewardType = rewardType,
        countryCode = countryCode,
        cashPaid = cashPaid,
        creditUsed = creditUsed,
        totalReturn = totalReturn,
        claimedAmount = claimedAmount,
        isFullyClaimed = isFullyClaimed ?: false,
        orderDate = orderDate,
        createdAt = createdAt,
        items = items,
    )
}

private fun toOrderItemResponse(record: com.teamrestocks.ttt.jooq.tables.records.OrderItemsRecord): OrderItemResponse {
    val id = checkNotNull(record.id) { "Order item id must not be null" }
    val productId = checkNotNull(record.productId) { "Order item $id has no product_id" }
    val quantity = checkNotNull(record.quantity) { "Order item $id has no quantity" }
    return OrderItemResponse(id, productId, quantity)
}

@Service
class OrderService(private val dsl: DSLContext) {

    @Transactional
    fun createOrder(userId: UUID, request: CreateOrderRequest): OrderResponse {
        val orderRecord = dsl.insertInto(ORDERS)
            .set(ORDERS.USER_ID, userId)
            .set(ORDERS.DEVICE_ID, request.deviceId)
            .set(ORDERS.SELLER_ID, request.sellerId)
            .set(ORDERS.ORDER_SN, request.orderSn)
            .set(ORDERS.EVENT_TYPE, request.eventType.wireName)
            .set(ORDERS.REWARD_TYPE, request.rewardType.wireName)
            .set(ORDERS.COUNTRY_CODE, request.countryCode.wireName)
            .set(ORDERS.CASH_PAID, request.cashPaid)
            .set(ORDERS.CREDIT_USED, request.creditUsed)
            .set(ORDERS.TOTAL_RETURN, request.totalReturn)
            .set(ORDERS.CLAIMED_AMOUNT, request.claimedAmount)
            .set(ORDERS.IS_FULLY_CLAIMED, request.isFullyClaimed)
            .apply { request.orderDate?.let { set(ORDERS.ORDER_DATE, it) } }
            .returning()
            .fetchOne()

        checkNotNull(orderRecord) { "Failed to insert order ${request.orderSn}" }

        val itemResponses = request.items.map { item: OrderItemRequest ->
            val itemRecord = dsl.insertInto(ORDER_ITEMS)
                .set(ORDER_ITEMS.ORDER_ID, orderRecord.id)
                .set(ORDER_ITEMS.PRODUCT_ID, item.productId)
                .set(ORDER_ITEMS.QUANTITY, item.quantity)
                .returning()
                .fetchOne()
            checkNotNull(itemRecord) { "Failed to insert order item for product ${item.productId}" }
            toOrderItemResponse(itemRecord)
        }

        return orderRecord.toOrderResponse(itemResponses)
    }

    fun findOrders(userId: UUID, isAdmin: Boolean): List<OrderResponse> {
        val orderRecords = dsl.selectFrom(ORDERS)
            .apply { if (!isAdmin) where(ORDERS.USER_ID.eq(userId)) }
            .orderBy(ORDERS.CREATED_AT.desc())
            .fetch()

        return orderRecords.map { order ->
            val items = dsl.selectFrom(ORDER_ITEMS)
                .where(ORDER_ITEMS.ORDER_ID.eq(order.id))
                .fetch()
                .map { toOrderItemResponse(it) }

            order.toOrderResponse(items)
        }
    }

    fun findOrderForUser(id: UUID, userId: UUID, isAdmin: Boolean): OrderResponse? {
        val order = dsl.selectFrom(ORDERS)
            .where(ORDERS.ID.eq(id))
            .apply { if (!isAdmin) and(ORDERS.USER_ID.eq(userId)) }
            .fetchOne() ?: return null

        val items = dsl.selectFrom(ORDER_ITEMS)
            .where(ORDER_ITEMS.ORDER_ID.eq(order.id))
            .fetch()
            .map { toOrderItemResponse(it) }

        return order.toOrderResponse(items)
    }

    fun deleteOrder(id: UUID, userId: UUID, isAdmin: Boolean): Int =
        dsl.deleteFrom(ORDERS)
            .where(ORDERS.ID.eq(id))
            .apply { if (!isAdmin) and(ORDERS.USER_ID.eq(userId)) }
            .execute()

    @Transactional
    fun updateClaimStatus(id: UUID, userId: UUID, isAdmin: Boolean, isFullyClaimed: Boolean, claimedAmount: java.math.BigDecimal): OrderResponse? {
        val updated = dsl.update(ORDERS)
            .set(ORDERS.IS_FULLY_CLAIMED, isFullyClaimed)
            .set(ORDERS.CLAIMED_AMOUNT, claimedAmount)
            .where(ORDERS.ID.eq(id))
            .apply { if (!isAdmin) and(ORDERS.USER_ID.eq(userId)) }
            .returning()
            .fetchOne() ?: return null

        val items = dsl.selectFrom(ORDER_ITEMS)
            .where(ORDER_ITEMS.ORDER_ID.eq(updated.id))
            .fetch()
            .map { toOrderItemResponse(it) }

        return updated.toOrderResponse(items)
    }
}

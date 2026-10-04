package at.endasy.ttt.service

import at.endasy.ttt.dto.CreateOrderRequest
import at.endasy.ttt.dto.OrderItemRequest
import at.endasy.ttt.dto.OrderItemResponse
import at.endasy.ttt.dto.OrderResponse
import com.teamrestocks.ttt.jooq.tables.references.ORDERS
import com.teamrestocks.ttt.jooq.tables.references.ORDER_ITEMS
import java.util.UUID
import org.jooq.DSLContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OrderService(private val dsl: DSLContext) {

    @Transactional
    fun createOrder(userId: UUID, request: CreateOrderRequest): OrderResponse {
        val orderRecord = dsl.insertInto(ORDERS)
            .set(ORDERS.USER_ID, userId)
            .set(ORDERS.DEVICE_ID, request.deviceId)
            .set(ORDERS.SELLER_ID, request.sellerId)
            .set(ORDERS.ORDER_SN, request.orderSn)
            .set(ORDERS.EVENT_TYPE, request.eventType)
            .set(ORDERS.CASH_PAID, request.cashPaid)
            .set(ORDERS.CREDIT_USED, request.creditUsed)
            .set(ORDERS.TOTAL_RETURN, request.totalReturn)
            .set(ORDERS.CLAIMED_AMOUNT, request.claimedAmount)
            .set(ORDERS.IS_FULLY_CLAIMED, request.isFullyClaimed)
            .apply { request.orderDate?.let { set(ORDERS.ORDER_DATE, it) } }
            .returning()
            .fetchOne()!!

        val itemResponses = request.items.map { item: OrderItemRequest ->
            val itemRecord = dsl.insertInto(ORDER_ITEMS)
                .set(ORDER_ITEMS.ORDER_ID, orderRecord.id)
                .set(ORDER_ITEMS.PRODUCT_ID, item.productId)
                .set(ORDER_ITEMS.QUANTITY, item.quantity)
                .returning()
                .fetchOne()!!
            OrderItemResponse(itemRecord.id!!, itemRecord.productId!!, itemRecord.quantity!!)
        }

        return OrderResponse(
            id = orderRecord.id!!,
            userId = orderRecord.userId!!,
            deviceId = orderRecord.deviceId,
            sellerId = orderRecord.sellerId,
            orderSn = orderRecord.orderSn!!,
            eventType = orderRecord.eventType,
            cashPaid = orderRecord.cashPaid!!,
            creditUsed = orderRecord.creditUsed!!,
            totalReturn = orderRecord.totalReturn!!,
            claimedAmount = orderRecord.claimedAmount!!,
            isFullyClaimed = orderRecord.isFullyClaimed ?: false,
            orderDate = orderRecord.orderDate,
            createdAt = orderRecord.createdAt,
            items = itemResponses,
        )
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
                .map { OrderItemResponse(it.id!!, it.productId!!, it.quantity!!) }

            OrderResponse(
                id = order.id!!,
                userId = order.userId!!,
                deviceId = order.deviceId,
                sellerId = order.sellerId,
                orderSn = order.orderSn!!,
                eventType = order.eventType,
                cashPaid = order.cashPaid!!,
                creditUsed = order.creditUsed!!,
                totalReturn = order.totalReturn!!,
                claimedAmount = order.claimedAmount!!,
                isFullyClaimed = order.isFullyClaimed ?: false,
                orderDate = order.orderDate,
                createdAt = order.createdAt,
                items = items,
            )
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
            .map { OrderItemResponse(it.id!!, it.productId!!, it.quantity!!) }

        return OrderResponse(
            id = order.id!!,
            userId = order.userId!!,
            deviceId = order.deviceId,
            sellerId = order.sellerId,
            orderSn = order.orderSn!!,
            eventType = order.eventType,
            cashPaid = order.cashPaid!!,
            creditUsed = order.creditUsed!!,
            totalReturn = order.totalReturn!!,
            claimedAmount = order.claimedAmount!!,
            isFullyClaimed = order.isFullyClaimed ?: false,
            orderDate = order.orderDate,
            createdAt = order.createdAt,
            items = items,
        )
    }

    fun deleteOrder(id: UUID, userId: UUID, isAdmin: Boolean): Int =
        dsl.deleteFrom(ORDERS)
            .where(ORDERS.ID.eq(id))
            .apply { if (!isAdmin) and(ORDERS.USER_ID.eq(userId)) }
            .execute()
}

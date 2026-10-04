package at.endasy.ttt.dto

import at.endasy.ttt.model.EventType
import at.endasy.ttt.model.RewardType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class OrderItemRequest(
    val productId: UUID,
    val quantity: Int = 1,
)

data class CreateOrderRequest(
    val deviceId: UUID?,
    val sellerId: UUID?,
    val orderSn: String,
    val eventType: EventType,
    val rewardType: RewardType,
    val cashPaid: BigDecimal = BigDecimal.ZERO,
    val creditUsed: BigDecimal = BigDecimal.ZERO,
    val totalReturn: BigDecimal,
    val claimedAmount: BigDecimal = BigDecimal.ZERO,
    val isFullyClaimed: Boolean = false,
    val orderDate: LocalDate? = null,
    val items: List<OrderItemRequest> = emptyList(),
)

data class UpdateClaimRequest(
    val isFullyClaimed: Boolean,
    val claimedAmount: BigDecimal,
)

data class OrderItemResponse(
    val id: UUID,
    val productId: UUID,
    val quantity: Int,
)

data class OrderResponse(
    val id: UUID,
    val userId: UUID,
    val deviceId: UUID?,
    val sellerId: UUID?,
    val orderSn: String,
    val eventType: EventType,
    val rewardType: RewardType,
    val cashPaid: BigDecimal,
    val creditUsed: BigDecimal,
    val totalReturn: BigDecimal,
    val claimedAmount: BigDecimal,
    val isFullyClaimed: Boolean,
    val orderDate: LocalDate?,
    val createdAt: OffsetDateTime?,
    val items: List<OrderItemResponse>,
)

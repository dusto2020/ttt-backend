package at.endasy.ttt.controller

import at.endasy.ttt.dto.CreateOrderRequest
import at.endasy.ttt.dto.UpdateClaimRequest
import at.endasy.ttt.security.TttOAuth2User
import at.endasy.ttt.security.requireAuth
import at.endasy.ttt.service.OrderService
import java.util.UUID
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/orders")
class OrderController(private val orderService: OrderService) {

    @GetMapping
    fun list(@AuthenticationPrincipal principal: TttOAuth2User?): ResponseEntity<Any> {
        requireAuth(principal)?.let { return it }
        return ResponseEntity.ok(orderService.findOrders(principal!!.userId, principal.isAdmin))
    }

    @GetMapping("/{id}")
    fun get(@AuthenticationPrincipal principal: TttOAuth2User?, @PathVariable id: UUID): ResponseEntity<Any> {
        requireAuth(principal)?.let { return it }
        val order = orderService.findOrderForUser(id, principal!!.userId, principal.isAdmin)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(order)
    }

    @PostMapping
    fun create(@AuthenticationPrincipal principal: TttOAuth2User?, @RequestBody body: CreateOrderRequest): ResponseEntity<Any> {
        requireAuth(principal)?.let { return it }
        return ResponseEntity.ok(orderService.createOrder(principal!!.userId, body))
    }

    @DeleteMapping("/{id}")
    fun delete(@AuthenticationPrincipal principal: TttOAuth2User?, @PathVariable id: UUID): ResponseEntity<Any> {
        requireAuth(principal)?.let { return it }
        val deleted = orderService.deleteOrder(id, principal!!.userId, principal.isAdmin)
        return if (deleted > 0) ResponseEntity.noContent().build() else ResponseEntity.notFound().build()
    }

    @PatchMapping("/{id}/claim")
    fun updateClaim(
        @AuthenticationPrincipal principal: TttOAuth2User?,
        @PathVariable id: UUID,
        @RequestBody body: UpdateClaimRequest,
    ): ResponseEntity<Any> {
        requireAuth(principal)?.let { return it }
        val updated = orderService.updateClaimStatus(id, principal!!.userId, principal.isAdmin, body.isFullyClaimed, body.claimedAmount)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(updated)
    }
}

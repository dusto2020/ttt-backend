package at.endasy.ttt.controller

import at.endasy.ttt.dto.MarkSoldRequest
import at.endasy.ttt.security.TttOAuth2User
import at.endasy.ttt.security.requireAuth
import at.endasy.ttt.service.InventoryService
import java.util.UUID
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/inventory")
class InventoryController(private val inventoryService: InventoryService) {

    @GetMapping
    fun list(
        @AuthenticationPrincipal principal: TttOAuth2User?,
        @RequestParam(required = false) status: String?,
    ): ResponseEntity<Any> {
        requireAuth(principal)?.let { return it }
        return ResponseEntity.ok(inventoryService.listInventoryForUser(principal!!.userId, status))
    }

    @GetMapping("/summary")
    fun summary(@AuthenticationPrincipal principal: TttOAuth2User?): ResponseEntity<Any> {
        requireAuth(principal)?.let { return it }
        return ResponseEntity.ok(inventoryService.getSummaryForUser(principal!!.userId))
    }

    @PostMapping("/{itemId}/sell")
    fun sell(
        @AuthenticationPrincipal principal: TttOAuth2User?,
        @PathVariable itemId: UUID,
        @RequestBody body: MarkSoldRequest,
    ): ResponseEntity<Any> {
        requireAuth(principal)?.let { return it }
        val ok = inventoryService.markAsSold(principal!!.userId, itemId, body.resalePrice, body.platform)
        return if (ok) ResponseEntity.ok(mapOf("success" to true)) else ResponseEntity.badRequest().build()
    }

    @PostMapping("/{itemId}/keep")
    fun keep(
        @AuthenticationPrincipal principal: TttOAuth2User?,
        @PathVariable itemId: UUID,
    ): ResponseEntity<Any> {
        requireAuth(principal)?.let { return it }
        val ok = inventoryService.markAsKept(principal!!.userId, itemId)
        return if (ok) ResponseEntity.ok(mapOf("success" to true)) else ResponseEntity.badRequest().build()
    }

    @PostMapping("/{itemId}/reopen")
    fun reopen(
        @AuthenticationPrincipal principal: TttOAuth2User?,
        @PathVariable itemId: UUID,
    ): ResponseEntity<Any> {
        requireAuth(principal)?.let { return it }
        val ok = inventoryService.markAsInStock(principal!!.userId, itemId)
        return if (ok) ResponseEntity.ok(mapOf("success" to true)) else ResponseEntity.badRequest().build()
    }
}
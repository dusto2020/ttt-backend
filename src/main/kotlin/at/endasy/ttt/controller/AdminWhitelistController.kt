package at.endasy.ttt.controller

import at.endasy.ttt.security.TttOAuth2User
import at.endasy.ttt.security.requireAdmin
import at.endasy.ttt.service.WhitelistService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

data class AddWhitelistRequest(
    val discordId: String,
    val username: String? = null,
    val notes: String? = null,
)

@RestController
@RequestMapping("/api/admin/whitelist")
class AdminWhitelistController(private val whitelistService: WhitelistService) {

    @GetMapping
    fun list(@AuthenticationPrincipal principal: TttOAuth2User?): ResponseEntity<Any> {
        requireAdmin(principal)?.let { return it }
        return ResponseEntity.ok(whitelistService.findAll())
    }

    @PostMapping
    fun add(
        @AuthenticationPrincipal principal: TttOAuth2User?,
        @RequestBody body: AddWhitelistRequest,
    ): ResponseEntity<Any> {
        requireAdmin(principal)?.let { return it }
        val ok = whitelistService.add(body.discordId, body.username, body.notes)
        return if (ok) ResponseEntity.ok(mapOf("success" to true)) else ResponseEntity.badRequest().build()
    }

    @DeleteMapping("/{discordId}")
    fun delete(
        @AuthenticationPrincipal principal: TttOAuth2User?,
        @PathVariable discordId: String,
    ): ResponseEntity<Any> {
        requireAdmin(principal)?.let { return it }
        val ok = whitelistService.remove(discordId)
        return if (ok) ResponseEntity.noContent().build() else ResponseEntity.notFound().build()
    }
}
package at.endasy.ttt.controller

import at.endasy.ttt.dto.SellerRequest
import at.endasy.ttt.dto.toResponse
import at.endasy.ttt.model.CountryCode
import at.endasy.ttt.security.TttOAuth2User
import at.endasy.ttt.security.requireAdmin
import at.endasy.ttt.service.SellerService
import java.util.UUID
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/sellers")
class AdminSellerController(private val sellerService: SellerService) {

    @GetMapping
    fun list(
        @AuthenticationPrincipal principal: TttOAuth2User?,
        @RequestParam(required = false) country: CountryCode?,
    ): ResponseEntity<Any> {
        requireAdmin(principal)?.let { return it }
        return ResponseEntity.ok(sellerService.findAll(country).map { it.toResponse() })
    }

    @PostMapping
    fun create(
        @AuthenticationPrincipal principal: TttOAuth2User?,
        @RequestBody body: SellerRequest,
    ): ResponseEntity<Any> {
        requireAdmin(principal)?.let { return it }
        return ResponseEntity.ok(
            sellerService.create(body.name, body.storeUrl, body.countryCode, body.isActive).toResponse()
        )
    }

    @PutMapping("/{id}")
    fun update(
        @AuthenticationPrincipal principal: TttOAuth2User?,
        @PathVariable id: UUID,
        @RequestBody body: SellerRequest,
    ): ResponseEntity<Any> {
        requireAdmin(principal)?.let { return it }
        val updated = sellerService.update(id, body.name, body.storeUrl, body.countryCode, body.isActive)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(updated.toResponse())
    }

    @DeleteMapping("/{id}")
    fun delete(@AuthenticationPrincipal principal: TttOAuth2User?, @PathVariable id: UUID): ResponseEntity<Any> {
        requireAdmin(principal)?.let { return it }
        val deleted = sellerService.delete(id)
        return if (deleted > 0) ResponseEntity.noContent().build() else ResponseEntity.notFound().build()
    }
}
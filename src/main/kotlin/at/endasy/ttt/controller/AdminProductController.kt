package at.endasy.ttt.controller

import at.endasy.ttt.dto.ProductRequest
import at.endasy.ttt.dto.toResponse
import at.endasy.ttt.security.TttOAuth2User
import at.endasy.ttt.security.requireAdmin
import at.endasy.ttt.service.ProductService
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
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/products")
class AdminProductController(private val productService: ProductService) {

    @GetMapping
    fun list(@AuthenticationPrincipal principal: TttOAuth2User?): ResponseEntity<Any> {
        requireAdmin(principal)?.let { return it }
        return ResponseEntity.ok(productService.findAll().map { it.toResponse() })
    }

    @PostMapping
    fun create(@AuthenticationPrincipal principal: TttOAuth2User?, @RequestBody body: ProductRequest): ResponseEntity<Any> {
        requireAdmin(principal)?.let { return it }
        return ResponseEntity.ok(
            productService.create(
                body.name,
                body.languageCode,
                body.temuAffiliateUrl,
                body.cardmarketUrl,
                body.temuPrice,
                body.cardmarketPrice,
                body.isActive,
            ).toResponse(),
        )
    }

    @PutMapping("/{id}")
    fun update(
        @AuthenticationPrincipal principal: TttOAuth2User?,
        @PathVariable id: UUID,
        @RequestBody body: ProductRequest,
    ): ResponseEntity<Any> {
        requireAdmin(principal)?.let { return it }
        val updated = productService.update(
            id,
            body.name,
            body.languageCode,
            body.temuAffiliateUrl,
            body.cardmarketUrl,
            body.temuPrice,
            body.cardmarketPrice,
            body.isActive,
        ) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(updated.toResponse())
    }

    @DeleteMapping("/{id}")
    fun delete(@AuthenticationPrincipal principal: TttOAuth2User?, @PathVariable id: UUID): ResponseEntity<Any> {
        requireAdmin(principal)?.let { return it }
        val deleted = productService.delete(id)
        return if (deleted > 0) ResponseEntity.noContent().build() else ResponseEntity.notFound().build()
    }

}

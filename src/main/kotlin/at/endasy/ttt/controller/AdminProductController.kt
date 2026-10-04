package at.endasy.ttt.controller

import at.endasy.ttt.dto.ProductRequest
import at.endasy.ttt.dto.TemuImportRequest
import at.endasy.ttt.dto.toResponse
import at.endasy.ttt.model.CountryCode
import at.endasy.ttt.model.LanguageCode
import at.endasy.ttt.security.TttOAuth2User
import at.endasy.ttt.security.requireAdmin
import at.endasy.ttt.service.CardmarketPriceSyncScheduler
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
import java.math.BigDecimal

@RestController
@RequestMapping("/api/admin/products")
class AdminProductController(
    private val productService: ProductService,
    private val cardmarketPriceSyncScheduler: CardmarketPriceSyncScheduler,
) {

    @GetMapping
    fun list(@AuthenticationPrincipal principal: TttOAuth2User?): ResponseEntity<Any> {
        requireAdmin(principal)?.let { return it }
        return ResponseEntity.ok(productService.findAll().map { it.toResponse() })
    }

    @PostMapping
    fun create(
        @AuthenticationPrincipal principal: TttOAuth2User?,
        @RequestBody body: ProductRequest
    ): ResponseEntity<Any> {
        requireAdmin(principal)?.let { return it }
        return ResponseEntity.ok(
            productService.create(
                body.name,
                body.languageCode,
                body.countryCode,
                body.temuAffiliateUrl,
                body.cardmarketUrl,
                body.temuPrice,
                body.isActive,
                body.manualPriceOverride,
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
            body.countryCode,
            body.temuAffiliateUrl,
            body.cardmarketUrl,
            body.temuPrice,
            body.isActive,
            body.manualPriceOverride,
        ) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(updated.toResponse())
    }

    @DeleteMapping("/{id}")
    fun delete(@AuthenticationPrincipal principal: TttOAuth2User?, @PathVariable id: UUID): ResponseEntity<Any> {
        requireAdmin(principal)?.let { return it }
        val deleted = productService.delete(id)
        return if (deleted > 0) ResponseEntity.noContent().build() else ResponseEntity.notFound().build()
    }

    @PostMapping("/{id}/sync-cm")
    fun syncCardmarket(@AuthenticationPrincipal principal: TttOAuth2User?, @PathVariable id: UUID): ResponseEntity<Any> {
        requireAdmin(principal)?.let { return it }
        val product = productService.findById(id) ?: return ResponseEntity.notFound().build()

        cardmarketPriceSyncScheduler.syncSingle(id, product.cardmarketUrl ?: "")

        val updated = productService.findById(id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(updated.toResponse())
    }

    @PostMapping("/sync-all-cm")
    fun syncAllCardmarket(@AuthenticationPrincipal principal: TttOAuth2User?): ResponseEntity<Any> {
        requireAdmin(principal)?.let { return it }
        // Startet den Scheduler asynchron im Hintergrund
        Thread {
            cardmarketPriceSyncScheduler.syncPrices()
        }.start()
        return ResponseEntity.ok(mapOf("message" to "Sync im Hintergrund gestartet"))
    }

    @PostMapping("/import-temu-json")
    fun importTemuJson(
        @AuthenticationPrincipal principal: TttOAuth2User?,
        @RequestBody body: TemuImportRequest,
    ): ResponseEntity<Any> {
        requireAdmin(principal)?.let { return it }

        val rawTitle = body.goodsInfo.title
        val temuPrice = BigDecimal(body.goodsInfo.priceInfo.price)
            .divide(BigDecimal(100), 2, java.math.RoundingMode.HALF_UP)

        // 1. Sprache erkennen
        val languageCode = when {
            rawTitle.contains("Deutsch", ignoreCase = true) ||
                    rawTitle.contains("German", ignoreCase = true) ||
                    rawTitle.contains("Top-Trainer-Box", ignoreCase = true) -> LanguageCode.DE

            rawTitle.contains("Japanisch", ignoreCase = true) ||
                    rawTitle.contains("Japanese", ignoreCase = true) ||
                    rawTitle.contains("Japan", ignoreCase = true) -> LanguageCode.JP

            else -> LanguageCode.EN
        }

        // 2. Land erkennen (Heartforcards Mall ID 670702094624648 = AT)
        val countryCode = if (body.goodsInfo.mallId == 670702094624648L) {
            CountryCode.AT
        } else {
            CountryCode.DE
        }

        // 3. Titel säubern
        val cleanName = cleanPokemonTitle(rawTitle, languageCode)

        // 4. Cardmarket-Suchlink vorbereiten (als Basis)
        val cmSearchQuery = java.net.URLEncoder.encode(cleanName.replace(Regex("^\\[(DE|EN|JP)\\]\\s*"), ""), "UTF-8")
        val cmUrl = "https://www.cardmarket.com/en/Pokemon/Products/Search?searchString=$cmSearchQuery"

        // 5. In Datenbank anlegen
        val created = productService.create(
            name = cleanName,
            languageCode = languageCode,
            countryCode = countryCode,
            temuAffiliateUrl = body.shortLink,
            cardmarketUrl = cmUrl,
            temuPrice = temuPrice,
            isActive = true,
            temuProductUrl = body.productUrl,
        )

        return ResponseEntity.ok(created.toResponse())
    }

    private fun cleanPokemonTitle(raw: String, lang: LanguageCode): String {
        // SEO-Müll wie "Collector's & Playset with Exclusive Contents" entfernen
        val cleaned = raw
            .replace(Regex("(?i)Pokémon\\s*TCG:?\\s*"), "")
            .replace(Regex("(?i)[–-]\\s*Collector.*"), "")
            .replace(Regex("(?i)[–-]\\s*Playset.*"), "")
            .replace(Regex("(?i)Sealed.*"), "")
            .replace(Regex("(?i)OVP.*"), "")
            .trim()

        return "[${lang.wireName}] $cleaned"
    }
}

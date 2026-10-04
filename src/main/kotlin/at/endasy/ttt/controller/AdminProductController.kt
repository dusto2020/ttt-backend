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
import at.endasy.ttt.service.SellerService
import java.math.BigDecimal
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
class AdminProductController(
    private val productService: ProductService,
    private val cardmarketPriceSyncScheduler: CardmarketPriceSyncScheduler,
    private val sellerService: SellerService, // <-- FIX 1: Hier injizieren!
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
                name = body.name,
                languageCode = body.languageCode,
                countryCode = body.countryCode,
                temuAffiliateUrl = body.temuAffiliateUrl,
                cardmarketUrl = body.cardmarketUrl,
                temuPrice = body.temuPrice,
                isActive = body.isActive,
                manualPriceOverride = body.manualPriceOverride,
                temuProductUrl = body.temuProductUrl, // <-- FIX: mitübergeben
                sellerId = body.sellerId,             // <-- FIX: mitübergeben
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
            id = id,
            name = body.name,
            languageCode = body.languageCode,
            countryCode = body.countryCode,
            temuAffiliateUrl = body.temuAffiliateUrl,
            cardmarketUrl = body.cardmarketUrl,
            temuPrice = body.temuPrice,
            isActive = body.isActive,
            manualPriceOverride = body.manualPriceOverride,
            temuProductUrl = body.temuProductUrl, // <-- FIX: mitübergeben
            sellerId = body.sellerId,             // <-- FIX: mitübergeben
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
    fun syncCardmarket(
        @AuthenticationPrincipal principal: TttOAuth2User?,
        @PathVariable id: UUID
    ): ResponseEntity<Any> {
        requireAdmin(principal)?.let { return it }
        val product = productService.findById(id) ?: return ResponseEntity.notFound().build()

        cardmarketPriceSyncScheduler.syncSingle(id, product.cardmarketUrl ?: "")

        val updated = productService.findById(id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(updated.toResponse())
    }

    @PostMapping("/sync-all-cm")
    fun syncAllCardmarket(@AuthenticationPrincipal principal: TttOAuth2User?): ResponseEntity<Any> {
        requireAdmin(principal)?.let { return it }
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

        // 1. Sprache dynamisch erkennen
        val languageCode = detectLanguage(rawTitle, body.productUrl ?: "")

        // 2. Land DYNAMISCH erkennen (aus Userscript, Seller-DB oder URL)
        val countryCode = body.countryCode
            ?: detectCountryFromSellerDb(body.goodsInfo.mallId)
            ?: detectCountryFromUrl(body.productUrl ?: body.shortLink)

        // 3. Händler VOLLAUTOMATISCH matchen
        val matchedSeller = findSellerByMallId(body.goodsInfo.mallId, countryCode)

        // 4. Titel säubern
        val cleanName = cleanPokemonTitle(rawTitle, languageCode)

        // 5. Cardmarket-Suchlink vorbereiten
        val cmSearchQuery = java.net.URLEncoder.encode(cleanName.replace(Regex("^\\[(DE|EN|JP)\\]\\s*"), ""), "UTF-8")
        val cmUrl = "https://www.cardmarket.com/de/Pokemon/Products/Search?searchString=$cmSearchQuery"

        // 6. In Datenbank anlegen (inkl. sellerId & temuProductUrl!)
        val created = productService.create(
            name = cleanName,
            languageCode = languageCode,
            countryCode = countryCode,
            temuAffiliateUrl = body.shortLink,
            cardmarketUrl = cmUrl,
            temuPrice = temuPrice,
            isActive = true,
            temuProductUrl = body.productUrl,
            sellerId = matchedSeller?.id,
        )

        return ResponseEntity.ok(created.toResponse())
    }

    private fun findSellerByMallId(mallId: Long, country: CountryCode): at.endasy.ttt.dto.SellerResponse? {
        val mallIdStr = mallId.toString()
        val allSellers = sellerService.findAll().map { it.toResponse() }

        // 1. Suche nach hinterlegter Store-URL mit dieser Mall-ID
        val byUrl = allSellers.firstOrNull { it.storeUrl?.contains(mallIdStr) == true }
        if (byUrl != null) return byUrl

        // 2. Fallback: Heartforcards für das passende Land finden
        return allSellers.firstOrNull {
            it.name.contains("Heartforcards", ignoreCase = true) && it.countryCode == country
        }
    }

    private fun detectCountryFromSellerDb(mallId: Long): CountryCode? {
        val mallIdStr = mallId.toString()
        val seller = sellerService.findAll().firstOrNull { it.storeUrl?.contains(mallIdStr) == true }
        return seller?.let {
            CountryCode.entries.find { c -> c.wireName == it.countryCode }
        }
    }

    private fun detectCountryFromUrl(url: String): CountryCode {
        val lower = url.lowercase()
        return if (lower.contains("/at-") || lower.contains("/at/") || lower.contains(".at")) {
            CountryCode.AT
        } else {
            CountryCode.DE
        }
    }

    private fun detectLanguage(title: String, url: String): LanguageCode {
        val combined = "$title $url".lowercase()
        return when {
            combined.contains("deutsch") ||
                    combined.contains("german") ||
                    combined.contains("top-trainer-box") -> LanguageCode.DE

            combined.contains("japanese") ||
                    combined.contains("japanisch") ||
                    combined.contains("japan") -> LanguageCode.JP

            else -> LanguageCode.EN
        }
    }

    private fun cleanPokemonTitle(raw: String, lang: LanguageCode): String {
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
package at.endasy.ttt.service

import com.fasterxml.jackson.databind.ObjectMapper
import java.math.BigDecimal
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

data class ScrapedCardmarketData(
    val price: BigDecimal?,
    val resolvedUrl: String?,
    val title: String?,
)

@Service
class CardmarketScraperService(
    @Value($$"${ttt.flaresolverr.url:http://localhost:8191/v1}")
    private val flareSolverrUrl: String,
    @Value($$"${serper.api-key:}")
    private val serperApiKey: String = ""
) {
    private val objectMapper = ObjectMapper()
    private val logger = LoggerFactory.getLogger(CardmarketScraperService::class.java)

    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .build()


    fun resolveCardmarketUrlViaGoogle(productName: String, languageCode: String? = null): String? {
        if (serperApiKey.isBlank()) {
            logger.warn("Serper API Key ist nicht konfiguriert in application.yml (ttt.serper.api-key)")
            return null
        }

        return try {
            var cleanQuery = productName
                .replace(Regex("^\\[(DE|EN|JP)]\\s*"), "")
                .replace(Regex("(?i)[–-]\\s*\\d+\\s*Booster.*"), "")
                .replace(Regex("(?i)English|Deutsch|German|Japanese"), "")
                .trim()

            if (!cleanQuery.startsWith("Pokemon", ignoreCase = true) && !cleanQuery.startsWith(
                    "Pokémon",
                    ignoreCase = true
                )
            ) {
                cleanQuery = "Pokemon $cleanQuery"
            }

            val fullSearchQuery = "$cleanQuery cardmarket"
            logger.info("Querying Google via Serper.dev for: '{}'", fullSearchQuery)

            val payload = mapOf(
                "q" to fullSearchQuery,
                "gl" to "de",
                "hl" to "de",
                "num" to 5
            )

            val request = HttpRequest.newBuilder()
                .uri(URI.create("https://google.serper.dev/search"))
                .header("X-API-KEY", serperApiKey)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() != 200) {
                logger.warn("Serper.dev returned HTTP {}: {}", response.statusCode(), response.body())
                return null
            }

            val rootNode = objectMapper.readTree(response.body())
            val organicArray = rootNode.path("organic")

            if (organicArray.isArray) {
                for (item in organicArray) {
                    val link = item.path("link").asText("")

                    // 1. Muss ein Cardmarket-Produktlink sein
                    val isCmProduct = link.contains("cardmarket.com/", ignoreCase = true) &&
                            link.contains("/Pokemon/Products/", ignoreCase = true)

                    // 2. KEINE Suchseiten und KEINE Einzelkarten (Singles)
                    val isSearchOrSingle = link.contains("/Search", ignoreCase = true) ||
                            link.contains("/Singles/", ignoreCase = true)

                    // 3. KEINE Kategorieseiten wie .../Products/Elite-Trainer-Boxes (muss echten Produktnamen haben!)
                    val pathAfterProducts = link.substringAfter("/Pokemon/Products/").substringBefore("?")
                    val hasProductSlug = pathAfterProducts.count { it == '/' } >= 1

                    if (isCmProduct && !isSearchOrSingle && hasProductSlug) {
                        val finalUrl = buildCardmarketUrlWithFilters(link, languageCode)
                        logger.info("Serper (Google) found Cardmarket Deep Link: {}", finalUrl)
                        return finalUrl
                    }
                }
            }

            logger.warn("Serper returned no Cardmarket product link for: '{}'", fullSearchQuery)
            null
        } catch (ex: Exception) {
            logger.warn("Failed to query Google via Serper.dev: {}", ex.message)
            null
        }
    }

    /**
     * Extrahiert den deutschen Titel aus dem <h1> und ignoriert Suchseiten
     */
    private fun extractGermanTitle(document: Document, url: String): String? {
        // NIEMALS Titel von Suchergebnisseiten übernehmen!
        if (url.contains("/Search", ignoreCase = true)) return null

        val h1 = document.selectFirst(".page-title-container h1")
            ?: document.selectFirst("h1")
            ?: return null

        val h1Clone = h1.clone()
        h1Clone.select("span").remove()

        val cleanTitle = h1Clone.text().trim()

        // Sicherheitsprüfung gegen Cardmarket-Suchseiten
        if (cleanTitle.equals("Suchergebnisse", ignoreCase = true) ||
            cleanTitle.equals("Search Results", ignoreCase = true) ||
            cleanTitle.isBlank()
        ) {
            return null
        }

        return cleanTitle
    }

    fun fetchLowestPrice(
        url: String?,
        productNameFallback: String? = null,
        languageCode: String? = null,
    ): ScrapedCardmarketData? {
        var targetUrl = url?.trim().orEmpty()
        var resolvedDeepLink: String? = null

        // 1. WENN DER LINK LEER IST ODER EIN SUCHLINK -> Über Google (Serper) suchen!
        if (targetUrl.isBlank() || targetUrl.contains("/Search", ignoreCase = true)) {
            val queryFromUrl = if (targetUrl.contains("searchString=")) {
                java.net.URLDecoder.decode(targetUrl.substringAfter("searchString=").substringBefore("&"), "UTF-8")
            } else {
                productNameFallback ?: ""
            }

            if (queryFromUrl.isBlank()) {
                logger.warn("Kann Cardmarket nicht durchsuchen: Sowohl URL als auch Produktname sind leer.")
                return null
            }

            logger.info("Kein gültiger Deeplink vorhanden. Starte Google-Suche für: '{}'", queryFromUrl)
            val foundUrl = resolveCardmarketUrlViaGoogle(queryFromUrl, languageCode)

            if (foundUrl != null) {
                targetUrl = foundUrl
                resolvedDeepLink = foundUrl
            } else {
                logger.warn("Google konnte keinen Cardmarket-Link für '{}' finden.", queryFromUrl)
                return null
            }
        }

        return try {
            // Exakte Sprach- und Länderfilter anfügen
            targetUrl = buildCardmarketUrlWithFilters(targetUrl, languageCode)
            resolvedDeepLink = targetUrl

            logger.info("Scraping Cardmarket (DE) with language filter: {}", targetUrl)

            val html = fetchHtmlViaFlareSolverr(targetUrl) ?: return null
            val document = Jsoup.parse(html)

            // 1. Deutschen Titel aus dem h1 extrahieren
            val germanTitle = extractGermanTitle(document, targetUrl)

            // 2. Preis ermitteln
            val price = extractPriceFromInfoList(document, listOf("Ab", "From"))
                ?: extractPriceFromFirstArticleRow(document)
                ?: extractPriceFromInfoList(document, listOf("Preistrend", "Price Trend"))

            if (price != null) {
                logger.info("Scraped price: {} € | Title: '{}' for {}", price, germanTitle, targetUrl)
            }

            ScrapedCardmarketData(
                price = price,
                resolvedUrl = resolvedDeepLink,
                title = germanTitle,
            )
        } catch (ex: Exception) {
            logger.warn("Failed to scrape Cardmarket price from {}: {}", targetUrl, ex.message)
            null
        }
    }

    private fun fetchHtmlViaFlareSolverr(url: String, cookies: List<Map<String, String>> = emptyList()): String? {
        val payload = mutableMapOf<String, Any>(
            "cmd" to "request.get",
            "url" to url,
            "maxTimeout" to 60000,
        )
        if (cookies.isNotEmpty()) {
            payload["cookies"] = cookies
        }

        val request = HttpRequest.newBuilder()
            .uri(URI.create(flareSolverrUrl))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(65))
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) return null

        val rootNode = objectMapper.readTree(response.body())
        if (rootNode.path("status").asText() != "ok") return null

        return rootNode.path("solution").path("response").asText()
    }

    private fun extractPriceFromInfoList(document: Document, targetLabels: List<String>): BigDecimal? {
        val infoContainer = document.selectFirst(".info-list-container") ?: return null
        val dtElements = infoContainer.select("dt")

        for (dt in dtElements) {
            val label = dt.text().trim()
            if (targetLabels.any { label.contains(it, ignoreCase = true) }) {
                val dd = dt.nextElementSibling()
                if (dd != null && dd.tagName().equals("dd", ignoreCase = true)) {
                    val price = parseGermanPrice(dd.text())
                    if (price != null) return price
                }
            }
        }
        return null
    }

    private fun extractPriceFromFirstArticleRow(document: Document): BigDecimal? {
        val firstPriceElem = document.selectFirst(".article-row .price-container span.color-primary")
            ?: document.selectFirst(".article-row span.color-primary")
        return firstPriceElem?.text()?.let { parseGermanPrice(it) }
    }

    private fun parseGermanPrice(raw: String): BigDecimal? {
        val match = PRICE_REGEX.find(raw) ?: return null
        val normalized = match.value
            .replace(".", "")
            .replace(",", ".")
        return try {
            BigDecimal(normalized)
        } catch (ex: NumberFormatException) {
            logger.warn("Failed to parse Cardmarket price '{}': {}", raw, ex.message)
            null
        }
    }

    fun buildCardmarketUrlWithFilters(url: String, languageCode: String?): String {
        val langId = when (languageCode?.uppercase()) {
            "DE" -> 3
            "JP" -> 7
            else -> 1 // Standard: Englisch
        }
        val baseUrl = url.substringBefore("?")
            .replace(Regex("/(en|fr|es|it)/Pokemon/"), "/de/Pokemon/")

        return "$baseUrl?sellerCountry=1,7&language=$langId"
    }

    /**
     * Zieht den Live-Preis direkt von der echten Temu-Produktseite (#goods_price)
     */
    fun fetchTemuPrice(productUrl: String): BigDecimal? {
        if (productUrl.isBlank()) return null

        return try {
            logger.info("Scraping live Temu price from: {}", productUrl)

            val html = fetchHtmlViaFlareSolverr(productUrl) ?: return null
            val doc = Jsoup.parse(html)

            // 1. Suche den Container mit der festen ID aus deinem Screenshot
            val goodsPriceDiv = doc.selectFirst("#goods_price") ?: doc.selectFirst("[class*=\"goods_price\"]")

            val rawText = if (goodsPriceDiv != null) {
                // Erstes <span> enthält das vollständige "€275.09"
                val span = goodsPriceDiv.selectFirst("span")
                span?.text() ?: goodsPriceDiv.text()
            } else {
                // Fallback: Schema.org JSON-LD
                doc.select("meta[property=\"og:price:amount\"]").attr("content")
            }

            val price = parseGermanPrice(rawText)
            if (price != null) {
                logger.info("Successfully scraped Temu live price: {} € for {}", price, productUrl)
            }
            price
        } catch (ex: Exception) {
            logger.warn("Failed to scrape Temu price from {}: {}", productUrl, ex.message)
            null
        }
    }

    companion object {
        private val PRICE_REGEX = Regex("""\d{1,3}(?:\.\d{3})*,\d{2}""")
    }
}
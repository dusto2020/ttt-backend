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

@Service
class CardmarketScraperService(
    @Value($$"${ttt.flaresolverr.url:http://localhost:8191/v1}")
    private val flareSolverrUrl: String,
) {
    private val objectMapper = ObjectMapper()

    private val logger = LoggerFactory.getLogger(CardmarketScraperService::class.java)

    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .build()

    fun fetchLowestPrice(url: String): BigDecimal? {
        if (url.isBlank()) return null

        return try {
            logger.info("Scraping Cardmarket via FlareSolverr: {}", url)

            val payload = mapOf(
                "cmd" to "request.get",
                "url" to url,
                "maxTimeout" to 60000
            )

            val request = HttpRequest.newBuilder()
                .uri(URI.create(flareSolverrUrl))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(65))
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() != 200) {
                logger.warn("FlareSolverr returned HTTP {}: {}", response.statusCode(), response.body())
                return null
            }

            val rootNode = objectMapper.readTree(response.body())
            val status = rootNode.path("status").asText()

            if (status != "ok") {
                logger.warn("FlareSolverr request failed: {}", rootNode.path("message").asText())
                return null
            }

            val html = rootNode.path("solution").path("response").asText()
            if (html.isBlank()) {
                logger.warn("FlareSolverr returned empty HTML for {}", url)
                return null
            }

            val document = Jsoup.parse(html)

            // 1. "From" / "Ab" im Info-Block
            val price = extractPriceFromInfoList(document, listOf("From", "Ab"))
            // 2. Fallback: Erste Zeile der Artikeltabelle
                ?: extractPriceFromFirstArticleRow(document)
                // 3. Fallback: Preistrend falls ausverkauft
                ?: extractPriceFromInfoList(document, listOf("Price Trend", "Preistrend"))

            if (price != null) {
                logger.info("Successfully scraped price: {} € for {}", price, url)
            } else {
                logger.warn("Could not find price in rendered HTML for {}", url)
            }

            price
        } catch (ex: Exception) {
            logger.warn("Failed to scrape Cardmarket price via FlareSolverr from {}: {}", url, ex.message)
            null
        }
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

    companion object {
        private val PRICE_REGEX = Regex("""\d{1,3}(?:\.\d{3})*,\d{2}""")
    }
}
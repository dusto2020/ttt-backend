package at.endasy.ttt.service

import java.math.BigDecimal
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class CardmarketScraperService {

    private val logger = LoggerFactory.getLogger(CardmarketScraperService::class.java)

    fun fetchLowestPrice(url: String): BigDecimal? {
        if (url.isBlank()) return null

        return try {
            val document = Jsoup.connect(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept-Language", "de-DE,de;q=0.9,en-US;q=0.8,en;q=0.7")
                .header("Referer", "https://www.cardmarket.com/")
                .timeout(12_000)
                .get()

            // 1. Try "From" / "Ab" in the info list container
            extractPriceFromInfoList(document, listOf("From", "Ab"))
            // 2. Fallback: First offer row in the article table
                ?: extractPriceFromFirstArticleRow(document)
                // 3. Fallback: "Price Trend" / "Preistrend" if out of stock
                ?: extractPriceFromInfoList(document, listOf("Price Trend", "Preistrend"))
        } catch (ex: Exception) {
            logger.warn("Failed to scrape Cardmarket price from {}: {}", url, ex.message)
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
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
        private val PRICE_REGEX = Regex("""\d{1,3}(?:\.\d{3})*,\d{2}""")
    }
}
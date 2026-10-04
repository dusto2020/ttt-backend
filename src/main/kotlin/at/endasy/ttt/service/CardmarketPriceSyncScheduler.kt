package at.endasy.ttt.service

import com.teamrestocks.ttt.jooq.tables.references.PRODUCTS
import java.time.Instant
import kotlin.random.Random
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class CardmarketPriceSyncScheduler(
    private val dsl: DSLContext,
    private val scraperService: CardmarketScraperService,
) {

    private val logger = LoggerFactory.getLogger(CardmarketPriceSyncScheduler::class.java)

    @Scheduled(cron = "0 0 */4 * * *")
    fun syncPrices() {
        val products = dsl.selectFrom(PRODUCTS)
            .where(PRODUCTS.IS_ACTIVE.isTrue)
            .and(PRODUCTS.CARDMARKET_URL.isNotNull)
            .fetch()

        logger.info("Starting Cardmarket price sync for {} products", products.size)

        products.forEachIndexed { index, product ->
            val url = product.cardmarketUrl ?: return@forEachIndexed
            syncSingle(product.id!!, url)

            if (index < products.size - 1) {
                Thread.sleep(Random.nextLong(2500, 5000))
            }
        }

        logger.info("Cardmarket price sync finished")
    }

    fun syncSingle(productId: java.util.UUID, url: String? = null) {
        val product = dsl.selectFrom(PRODUCTS).where(PRODUCTS.ID.eq(productId)).fetchOne() ?: return
        val targetUrl = url ?: product.cardmarketUrl ?: ""

        val result = scraperService.fetchLowestPrice(targetUrl, product.name, product.languageCode) ?: return

        val price = result.price
        val resolvedDeepLink = result.resolvedUrl
        val germanTitle = result.title

        var updateQuery = dsl.update(PRODUCTS)
            .set(PRODUCTS.CARDMARKET_UPDATED_AT, java.time.OffsetDateTime.ofInstant(Instant.now(), java.time.ZoneOffset.UTC))

        if (price != null) {
            updateQuery = updateQuery.set(PRODUCTS.CARDMARKET_LOWEST_PRICE, price)
        }
        if (resolvedDeepLink != null) {
            updateQuery = updateQuery.set(PRODUCTS.CARDMARKET_URL, resolvedDeepLink)
        }
        if (germanTitle != null) {
            updateQuery = updateQuery.set(PRODUCTS.NAME, germanTitle)
        }

        updateQuery.where(PRODUCTS.ID.eq(productId)).execute()
    }
}

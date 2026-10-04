package at.endasy.ttt.service

import at.endasy.ttt.dto.DealDto
import com.teamrestocks.ttt.jooq.tables.references.PRODUCTS
import java.math.RoundingMode
import org.jooq.DSLContext
import org.springframework.stereotype.Service

@Service
class DealService(private val dsl: DSLContext) {

    fun findActiveDeals(): List<DealDto> =
        dsl.selectFrom(PRODUCTS)
            .where(PRODUCTS.IS_ACTIVE.isTrue)
            .orderBy(PRODUCTS.NAME)
            .fetch()
            .map { product ->
                val spreadRatio = if (product.temuPrice != null && product.temuPrice!!.signum() != 0) {
                    product.cardmarketPrice!!.divide(product.temuPrice, 4, RoundingMode.HALF_UP)
                } else {
                    null
                }
                DealDto(
                    id = product.id!!,
                    name = product.name!!,
                    languageCode = product.languageCode!!,
                    temuAffiliateUrl = product.temuAffiliateUrl!!,
                    cardmarketUrl = product.cardmarketUrl!!,
                    temuPrice = product.temuPrice!!,
                    cardmarketPrice = product.cardmarketPrice!!,
                    spreadRatio = spreadRatio,
                )
            }
}

package at.endasy.ttt.service

import at.endasy.ttt.model.CountryCode
import at.endasy.ttt.model.LanguageCode
import com.teamrestocks.ttt.jooq.tables.records.ProductsRecord
import com.teamrestocks.ttt.jooq.tables.references.PRODUCTS
import java.math.BigDecimal
import java.util.UUID
import org.jooq.DSLContext
import org.springframework.stereotype.Service

@Service
class ProductService(private val dsl: DSLContext) {

    fun findAll(): List<ProductsRecord> =
        dsl.selectFrom(PRODUCTS).orderBy(PRODUCTS.NAME).fetch()

    fun findActive(): List<ProductsRecord> =
        dsl.selectFrom(PRODUCTS).where(PRODUCTS.IS_ACTIVE.isTrue).orderBy(PRODUCTS.NAME).fetch()

    fun findById(id: UUID): ProductsRecord? =
        dsl.selectFrom(PRODUCTS).where(PRODUCTS.ID.eq(id)).fetchOne()

    fun create(
        name: String,
        languageCode: LanguageCode,
        countryCode: CountryCode,
        temuAffiliateUrl: String,
        cardmarketUrl: String,
        temuPrice: BigDecimal,
        cardmarketPrice: BigDecimal,
        isActive: Boolean,
        manualPriceOverride: BigDecimal? = null,
    ): ProductsRecord =
        dsl.insertInto(PRODUCTS)
            .set(PRODUCTS.NAME, name)
            .set(PRODUCTS.LANGUAGE_CODE, languageCode.wireName)
            .set(PRODUCTS.COUNTRY_CODE, countryCode.wireName)
            .set(PRODUCTS.TEMU_AFFILIATE_URL, temuAffiliateUrl)
            .set(PRODUCTS.CARDMARKET_URL, cardmarketUrl)
            .set(PRODUCTS.TEMU_PRICE, temuPrice)
            .set(PRODUCTS.CARDMARKET_PRICE, cardmarketPrice)
            .set(PRODUCTS.IS_ACTIVE, isActive)
            .set(PRODUCTS.MANUAL_PRICE_OVERRIDE, manualPriceOverride)
            .returning()
            .fetchOne()!!

    fun update(
        id: UUID,
        name: String,
        languageCode: LanguageCode,
        countryCode: CountryCode,
        temuAffiliateUrl: String,
        cardmarketUrl: String,
        temuPrice: BigDecimal,
        cardmarketPrice: BigDecimal,
        isActive: Boolean,
        manualPriceOverride: BigDecimal? = null,
    ): ProductsRecord? =
        dsl.update(PRODUCTS)
            .set(PRODUCTS.NAME, name)
            .set(PRODUCTS.LANGUAGE_CODE, languageCode.wireName)
            .set(PRODUCTS.COUNTRY_CODE, countryCode.wireName)
            .set(PRODUCTS.TEMU_AFFILIATE_URL, temuAffiliateUrl)
            .set(PRODUCTS.CARDMARKET_URL, cardmarketUrl)
            .set(PRODUCTS.TEMU_PRICE, temuPrice)
            .set(PRODUCTS.CARDMARKET_PRICE, cardmarketPrice)
            .set(PRODUCTS.IS_ACTIVE, isActive)
            .set(PRODUCTS.MANUAL_PRICE_OVERRIDE, manualPriceOverride)
            .set(PRODUCTS.UPDATED_AT, org.jooq.impl.DSL.currentOffsetDateTime())
            .where(PRODUCTS.ID.eq(id))
            .returning()
            .fetchOne()

    fun delete(id: UUID): Int =
        dsl.deleteFrom(PRODUCTS).where(PRODUCTS.ID.eq(id)).execute()
}

package at.endasy.ttt.service

import at.endasy.ttt.model.CountryCode
import com.teamrestocks.ttt.jooq.tables.records.SellersRecord
import com.teamrestocks.ttt.jooq.tables.references.SELLERS
import java.util.UUID
import org.jooq.DSLContext
import org.springframework.stereotype.Service

@Service
class SellerService(private val dsl: DSLContext) {

    fun findAll(countryCode: CountryCode? = null): List<SellersRecord> =
        dsl.selectFrom(SELLERS)
            .apply { countryCode?.let { where(SELLERS.COUNTRY_CODE.eq(it.wireName)) } }
            .orderBy(SELLERS.NAME)
            .fetch()

    fun findActive(countryCode: CountryCode? = null): List<SellersRecord> =
        dsl.selectFrom(SELLERS)
            .where(SELLERS.IS_ACTIVE.isTrue)
            .apply { countryCode?.let { and(SELLERS.COUNTRY_CODE.eq(it.wireName)) } }
            .orderBy(SELLERS.NAME)
            .fetch()

    fun findById(id: UUID): SellersRecord? =
        dsl.selectFrom(SELLERS).where(SELLERS.ID.eq(id)).fetchOne()

    fun create(name: String, storeUrl: String?, countryCode: CountryCode, isActive: Boolean): SellersRecord =
        dsl.insertInto(SELLERS)
            .set(SELLERS.NAME, name)
            .set(SELLERS.STORE_URL, storeUrl)
            .set(SELLERS.COUNTRY_CODE, countryCode.wireName)
            .set(SELLERS.IS_ACTIVE, isActive)
            .returning()
            .fetchOne()!!

    fun update(id: UUID, name: String, storeUrl: String?, countryCode: CountryCode, isActive: Boolean): SellersRecord? =
        dsl.update(SELLERS)
            .set(SELLERS.NAME, name)
            .set(SELLERS.STORE_URL, storeUrl)
            .set(SELLERS.COUNTRY_CODE, countryCode.wireName)
            .set(SELLERS.IS_ACTIVE, isActive)
            .where(SELLERS.ID.eq(id))
            .returning()
            .fetchOne()

    fun delete(id: UUID): Int =
        dsl.deleteFrom(SELLERS).where(SELLERS.ID.eq(id)).execute()
}
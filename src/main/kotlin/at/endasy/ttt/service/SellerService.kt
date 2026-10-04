package at.endasy.ttt.service

import com.teamrestocks.ttt.jooq.tables.records.SellersRecord
import com.teamrestocks.ttt.jooq.tables.references.SELLERS
import java.util.UUID
import org.jooq.DSLContext
import org.springframework.stereotype.Service

@Service
class SellerService(private val dsl: DSLContext) {

    fun findAll(): List<SellersRecord> =
        dsl.selectFrom(SELLERS).orderBy(SELLERS.NAME).fetch()

    fun findActive(): List<SellersRecord> =
        dsl.selectFrom(SELLERS).where(SELLERS.IS_ACTIVE.isTrue).orderBy(SELLERS.NAME).fetch()

    fun findById(id: UUID): SellersRecord? =
        dsl.selectFrom(SELLERS).where(SELLERS.ID.eq(id)).fetchOne()

    fun create(name: String, storeUrl: String?, isActive: Boolean): SellersRecord =
        dsl.insertInto(SELLERS)
            .set(SELLERS.NAME, name)
            .set(SELLERS.STORE_URL, storeUrl)
            .set(SELLERS.IS_ACTIVE, isActive)
            .returning()
            .fetchOne()!!

    fun update(id: UUID, name: String, storeUrl: String?, isActive: Boolean): SellersRecord? =
        dsl.update(SELLERS)
            .set(SELLERS.NAME, name)
            .set(SELLERS.STORE_URL, storeUrl)
            .set(SELLERS.IS_ACTIVE, isActive)
            .where(SELLERS.ID.eq(id))
            .returning()
            .fetchOne()

    fun delete(id: UUID): Int =
        dsl.deleteFrom(SELLERS).where(SELLERS.ID.eq(id)).execute()
}

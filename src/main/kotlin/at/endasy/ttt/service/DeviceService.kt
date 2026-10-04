package at.endasy.ttt.service

import com.teamrestocks.ttt.jooq.tables.records.UserDevicesRecord
import com.teamrestocks.ttt.jooq.tables.references.USER_DEVICES
import java.util.UUID
import org.jooq.DSLContext
import org.springframework.stereotype.Service

@Service
class DeviceService(private val dsl: DSLContext) {

    fun findAllForUser(userId: UUID): List<UserDevicesRecord> =
        dsl.selectFrom(USER_DEVICES).where(USER_DEVICES.USER_ID.eq(userId)).orderBy(USER_DEVICES.LABEL).fetch()

    fun findByIdForUser(id: UUID, userId: UUID): UserDevicesRecord? =
        dsl.selectFrom(USER_DEVICES)
            .where(USER_DEVICES.ID.eq(id))
            .and(USER_DEVICES.USER_ID.eq(userId))
            .fetchOne()

    fun create(userId: UUID, label: String): UserDevicesRecord =
        dsl.insertInto(USER_DEVICES)
            .set(USER_DEVICES.USER_ID, userId)
            .set(USER_DEVICES.LABEL, label)
            .returning()
            .fetchOne()!!

    fun update(id: UUID, userId: UUID, label: String): UserDevicesRecord? =
        dsl.update(USER_DEVICES)
            .set(USER_DEVICES.LABEL, label)
            .where(USER_DEVICES.ID.eq(id))
            .and(USER_DEVICES.USER_ID.eq(userId))
            .returning()
            .fetchOne()

    fun delete(id: UUID, userId: UUID): Int =
        dsl.deleteFrom(USER_DEVICES)
            .where(USER_DEVICES.ID.eq(id))
            .and(USER_DEVICES.USER_ID.eq(userId))
            .execute()
}

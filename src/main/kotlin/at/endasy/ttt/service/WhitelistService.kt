package at.endasy.ttt.service

import com.teamrestocks.ttt.jooq.tables.references.WHITELISTED_USERS
import java.time.OffsetDateTime
import java.time.ZoneOffset
import org.jooq.DSLContext
import org.springframework.stereotype.Service

data class WhitelistEntry(
    val discordId: String,
    val discordUsername: String?,
    val notes: String?,
    val createdAt: OffsetDateTime?,
)

@Service
class WhitelistService(private val dsl: DSLContext) {

    fun isWhitelisted(discordId: String): Boolean =
        dsl.fetchExists(
            dsl.selectFrom(WHITELISTED_USERS).where(WHITELISTED_USERS.DISCORD_ID.eq(discordId))
        )

    fun findAll(): List<WhitelistEntry> =
        dsl.selectFrom(WHITELISTED_USERS)
            .orderBy(WHITELISTED_USERS.CREATED_AT.desc())
            .fetch { r ->
                WhitelistEntry(
                    discordId = r.discordId!!,
                    discordUsername = r.discordUsername,
                    notes = r.notes,
                    createdAt = r.createdAt,
                )
            }

    fun add(discordId: String, username: String? = null, notes: String? = null): Boolean {
        val cleanId = discordId.trim()
        if (cleanId.isBlank()) return false

        val inserted = dsl.insertInto(WHITELISTED_USERS)
            .set(WHITELISTED_USERS.DISCORD_ID, cleanId)
            .set(WHITELISTED_USERS.DISCORD_USERNAME, username?.trim())
            .set(WHITELISTED_USERS.NOTES, notes?.trim())
            .onConflict(WHITELISTED_USERS.DISCORD_ID)
            .doUpdate()
            .set(WHITELISTED_USERS.NOTES, notes?.trim())
            .execute()

        return inserted > 0
    }

    fun remove(discordId: String): Boolean =
        dsl.deleteFrom(WHITELISTED_USERS)
            .where(WHITELISTED_USERS.DISCORD_ID.eq(discordId.trim()))
            .execute() > 0
}
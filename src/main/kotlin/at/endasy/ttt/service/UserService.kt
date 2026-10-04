package at.endasy.ttt.service

import com.teamrestocks.ttt.jooq.tables.references.USERS
import org.jooq.DSLContext
import org.springframework.stereotype.Service

@Service
class UserService(private val dsl: DSLContext) {

    /**
     * Upserts a Discord-authenticated user by their Discord ID.
     * On conflict, refreshes the username and avatar URL while preserving `is_admin`.
     */
    fun upsertDiscordUser(discordId: String, discordUsername: String, avatarUrl: String?) =
        dsl.insertInto(USERS)
            .set(USERS.DISCORD_ID, discordId)
            .set(USERS.DISCORD_USERNAME, discordUsername)
            .set(USERS.AVATAR_URL, avatarUrl)
            .onConflict(USERS.DISCORD_ID)
            .doUpdate()
            .set(USERS.DISCORD_USERNAME, discordUsername)
            .set(USERS.AVATAR_URL, avatarUrl)
            .returning()
            .fetchOne()!!

    fun findByDiscordId(discordId: String) =
        dsl.selectFrom(USERS)
            .where(USERS.DISCORD_ID.eq(discordId))
            .fetchOne()
}

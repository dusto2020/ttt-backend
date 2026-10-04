package at.endasy.ttt.security

import at.endasy.ttt.config.DiscordProperties
import at.endasy.ttt.service.UserService
import at.endasy.ttt.service.WhitelistService
import org.slf4j.LoggerFactory
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

/**
 * Custom [OAuth2UserService] for Discord that gates access to:
 * 1. Members of [DiscordProperties.targetGuildId] (TeamRestocks Discord).
 * 2. Whitelisted Beta users OR Admins.
 */
@Service
class DiscordOAuth2UserService(
    private val discordProperties: DiscordProperties,
    private val userService: UserService,
    private val whitelistService: WhitelistService,
) : OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private val logger = LoggerFactory.getLogger(DiscordOAuth2UserService::class.java)
    private val delegate = DefaultOAuth2UserService()
    private val restClient = RestClient.create()

    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        val oAuth2User = delegate.loadUser(userRequest)
        val accessToken = userRequest.accessToken.tokenValue


        val guilds = try {
            restClient.get()
                .uri("https://discord.com/api/users/@me/guilds")
                .header("Authorization", "Bearer $accessToken")
                .retrieve()
                .body(Array<DiscordGuild>::class.java)
                ?: emptyArray()
        } catch (ex: RestClientException) {
            throw OAuth2AuthenticationException(
                OAuth2Error("guilds_fetch_failed", "Unable to verify Discord guild membership", null),
                ex,
            )
        }

        val isMember = guilds.any { it.id == discordProperties.targetGuildId }
        if (!isMember) {
            logger.warn("Login verweigert: User ist nicht auf dem TeamRestocks Discord Server.")
            throw OAuth2AuthenticationException(
                OAuth2Error(
                    "not_guild_member",
                    "Access restricted to TeamRestocks Discord members",
                    null,
                ),
            )
        }


        val discordId = oAuth2User.getAttribute<String>("id")
            ?: throw OAuth2AuthenticationException(OAuth2Error("invalid_user", "Missing Discord user id", null))
        val username = oAuth2User.getAttribute<String>("username") ?: discordId
        val avatarHash = oAuth2User.getAttribute<String>("avatar")
        val avatarUrl = avatarHash?.let { "https://cdn.discordapp.com/avatars/$discordId/$it.png" }

        val userRecord = userService.upsertDiscordUser(discordId, username, avatarUrl)
        val isAdmin = userRecord.isAdmin ?: false


        val isWhitelisted = whitelistService.isWhitelisted(discordId)

        if (!isAdmin && !isWhitelisted) {
            logger.warn("Login verweigert: User '{}' (Discord-ID: {}) ist nicht für die Beta freigeschaltet.", username, discordId)
            throw OAuth2AuthenticationException(
                OAuth2Error(
                    "not_whitelisted",
                    "Du bist noch nicht für die Beta freigeschaltet. Bitte wende dich an das TeamRestocks-Team.",
                    null,
                ),
            )
        }

        logger.info("Erfolgreicher Login für User '{}' (ID: {}, Admin: {}, Whitelisted: {})", username, discordId, isAdmin, isWhitelisted)

        return TttOAuth2User(
            delegate = oAuth2User,
            userId = userRecord.id!!,
            discordId = userRecord.discordId!!,
            discordUsername = userRecord.discordUsername!!,
            avatarUrl = userRecord.avatarUrl,
            isAdmin = isAdmin,
        )
    }

    private data class DiscordGuild(val id: String? = null)
}
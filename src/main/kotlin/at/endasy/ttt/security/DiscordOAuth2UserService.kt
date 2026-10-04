package at.endasy.ttt.security

import at.endasy.ttt.config.DiscordProperties
import at.endasy.ttt.service.UserService
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
 * Custom [OAuth2UserService] for Discord that gates access to members of [DiscordProperties.targetGuildId].
 */
@Service
class DiscordOAuth2UserService(
    private val discordProperties: DiscordProperties,
    private val userService: UserService,
) : OAuth2UserService<OAuth2UserRequest, OAuth2User> {

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

        return TttOAuth2User(
            delegate = oAuth2User,
            userId = userRecord.id!!,
            discordId = userRecord.discordId!!,
            discordUsername = userRecord.discordUsername!!,
            avatarUrl = userRecord.avatarUrl,
            isAdmin = userRecord.isAdmin ?: false,
        )
    }

    private data class DiscordGuild(val id: String? = null)
}

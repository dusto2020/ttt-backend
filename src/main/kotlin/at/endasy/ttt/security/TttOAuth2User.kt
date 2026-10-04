package at.endasy.ttt.security

import java.util.UUID
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.core.user.OAuth2User

/**
 * Custom [OAuth2User] wrapping the persisted application user record.
 */
class TttOAuth2User(
    private val delegate: OAuth2User,
    val userId: UUID,
    val discordId: String,
    val discordUsername: String,
    val avatarUrl: String?,
    val isAdmin: Boolean,
) : OAuth2User {

    override fun getName(): String = discordId

    override fun getAttributes(): Map<String, Any> = delegate.attributes

    override fun getAuthorities(): Collection<GrantedAuthority> = delegate.authorities
}

package at.endasy.ttt.controller

import at.endasy.ttt.security.TttOAuth2User
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController {

    @GetMapping("/me")
    fun me(@AuthenticationPrincipal principal: TttOAuth2User?): ResponseEntity<Any> {
        if (principal == null) {
            return ResponseEntity.status(401).build()
        }

        return ResponseEntity.ok(
            UserProfileResponse(
                id = principal.userId,
                discordId = principal.discordId,
                discordUsername = principal.discordUsername,
                avatarUrl = principal.avatarUrl,
                isAdmin = principal.isAdmin,
            ),
        )
    }

    data class UserProfileResponse(
        val id: java.util.UUID,
        val discordId: String,
        val discordUsername: String,
        val avatarUrl: String?,
        val isAdmin: Boolean,
    )
}

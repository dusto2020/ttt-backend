package at.endasy.ttt.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "discord")
data class DiscordProperties(
    val targetGuildId: String,
    val clientId: String,
    val clientSecret: String,
)

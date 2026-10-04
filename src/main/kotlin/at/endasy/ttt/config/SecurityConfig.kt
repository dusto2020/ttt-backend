package at.endasy.ttt.config

import at.endasy.ttt.security.DiscordOAuth2UserService
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configurers.oauth2.client.OAuth2LoginConfigurer
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.http.HttpStatus
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession

@Configuration
@EnableJdbcHttpSession(maxInactiveIntervalInSeconds = 14 * 24 * 60 * 60)
class SecurityConfig(
    private val discordOAuth2UserService: DiscordOAuth2UserService,
    @Value("\${app.frontend-url:http://localhost:5173}") private val frontendUrl: String,
) {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/api/auth/**", "/oauth2/**", "/login/**").permitAll()
                    .anyRequest().authenticated()
            }
            .oauth2Login { oauth2: OAuth2LoginConfigurer<HttpSecurity> ->
                oauth2.userInfoEndpoint { it.userService(discordOAuth2UserService) }
                oauth2.defaultSuccessUrl("$frontendUrl/", true)
                oauth2.failureUrl("$frontendUrl/unauthorized")
            }
            .exceptionHandling { it.authenticationEntryPoint(HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)) }
            .csrf { it.disable() }
            .sessionManagement { it.sessionFixation().newSession() }

        return http.build()
    }
}

package at.endasy.ttt.config

import at.endasy.ttt.security.DiscordOAuth2UserService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configurers.oauth2.client.OAuth2LoginConfigurer
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.http.HttpStatus

@Configuration
class SecurityConfig(private val discordOAuth2UserService: DiscordOAuth2UserService) {

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
            }
            .exceptionHandling { it.authenticationEntryPoint(HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)) }
            .csrf { it.disable() }
            .sessionManagement { it.sessionFixation().newSession() }

        return http.build()
    }
}

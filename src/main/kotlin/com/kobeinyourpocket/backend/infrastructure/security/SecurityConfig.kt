package com.kobeinyourpocket.backend.infrastructure.security

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.web.SecurityFilterChain
import javax.crypto.spec.SecretKeySpec

/**
 * Supabase JWT を検証する Resource Server（#89-a）。
 *
 * - 自前のログイン・トークン発行はしない（Supabase Auth が正）
 * - 検証方式: HS256 + [SupabaseJwtProperties.secret]（`SUPABASE_JWT_SECRET`）
 * - 認可: 現状は全エンドポイント permitAll（#90 で運営書き込みを締める）
 * - Bearer が付いていれば署名検証し、[SupabaseJwtAuthenticationConverter] で authorities を付与
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(SupabaseJwtProperties::class)
class SecurityConfig(
    private val supabaseJwtProperties: SupabaseJwtProperties,
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                // Q5=(A): #89 では閲覧・書き込みともオープン。#90 で POST 等を operator 限定にする。
                auth.anyRequest().permitAll()
            }.oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.jwtAuthenticationConverter(SupabaseJwtAuthenticationConverter())
                }
            }
        return http.build()
    }

    @Bean
    fun jwtDecoder(): JwtDecoder {
        val secret = supabaseJwtProperties.secret
        require(secret.isNotBlank()) {
            "supabase.jwt.secret (SUPABASE_JWT_SECRET) must be set to verify Supabase JWTs"
        }
        val key = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256")
        return NimbusJwtDecoder
            .withSecretKey(key)
            .macAlgorithm(MacAlgorithm.HS256)
            .build()
    }
}

package com.kobeinyourpocket.backend.infrastructure.security

import com.kobeinyourpocket.backend.infrastructure.supabase.SupabaseAuthProperties
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
 * - 検証方式: プロジェクトの Signing Keys（ES256 等）を JWKS で検証。
 *   `SUPABASE_URL` が空のときだけ JWT Secret（HS256）にフォールバック（主にテスト）
 * - 認可: 現状は全エンドポイント permitAll（#90 で運営書き込みを締める）
 * - Bearer が付いていれば署名検証し、[SupabaseJwtAuthenticationConverter] で authorities を付与
 *
 * 注意: [NimbusJwtDecoder.withJwkSetUri] の既定は RS256 のみ。
 * Supabase Signing Keys（ES256）では [NimbusJwtDecoder.JwkSetUriJwtDecoderBuilder.discoverJwsAlgorithms] が必要。
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
    private val supabaseJwtProperties: SupabaseJwtProperties,
    private val supabaseAuthProperties: SupabaseAuthProperties,
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
        val baseUrl = supabaseAuthProperties.url.trim().trimEnd('/')
        if (baseUrl.isNotBlank()) {
            // 例: https://xxxx.supabase.co/auth/v1/.well-known/jwks.json
            // 既定の RS256 だけでは ES256（Supabase Signing Keys）を検証できない。
            // JWKS 上の alg を使う（現状 ES256）。
            return NimbusJwtDecoder
                .withJwkSetUri("$baseUrl/auth/v1/.well-known/jwks.json")
                .discoverJwsAlgorithms()
                .build()
        }

        val secret = supabaseJwtProperties.secret
        require(secret.isNotBlank()) {
            "supabase.url (SUPABASE_URL) or supabase.jwt.secret (SUPABASE_JWT_SECRET) must be set to verify Supabase JWTs"
        }
        val key = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256")
        return NimbusJwtDecoder
            .withSecretKey(key)
            .macAlgorithm(MacAlgorithm.HS256)
            .build()
    }
}

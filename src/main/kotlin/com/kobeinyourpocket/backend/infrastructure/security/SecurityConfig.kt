package com.kobeinyourpocket.backend.infrastructure.security

import com.kobeinyourpocket.backend.domain.user.vo.Role
import com.kobeinyourpocket.backend.infrastructure.supabase.SupabaseAuthProperties
import jakarta.servlet.DispatcherType
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.access.hierarchicalroles.RoleHierarchy
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl
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
 * Supabase JWT を検証する Resource Server（#89-a）と書き込み認可（#90）。
 *
 * - 自前のログイン・トークン発行はしない（Supabase Auth が正）
 * - 検証方式: プロジェクトの Signing Keys（ES256 等）を JWKS で検証。
 *   `SUPABASE_URL` が空のときだけ JWT Secret（HS256）にフォールバック（主にテスト）
 * - 認可（#90）: 書き込みは deny-by-default。
 *   - GET 系は公開
 *   - `POST /api/v1/auth/signup|login|google|refresh` は公開（認証の入り口）
 *   - `POST /api/v1/auth/logout` とレビュー投稿・更新は認証必須（一般ロール可）
 *   - 上記以外の書き込み（`POST /api/v1/tourism/spots` 等、今後追加分も含む）は
 *     運営（OPERATOR）ロール必須
 * - ロール階層: ADMIN > OPERATOR > GENERAL（[roleHierarchy]）。
 *   ADMIN は運営系書き込みも実行できる
 * - 401/403 は [ApiAuthenticationEntryPoint] / [ApiAccessDeniedHandler] で
 *   統一エラー形式（§3.3 / #24）に差し替える
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
    fun securityFilterChain(
        http: HttpSecurity,
        authenticationEntryPoint: ApiAuthenticationEntryPoint,
        accessDeniedHandler: ApiAccessDeniedHandler,
    ): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    // 例外時の /error への ERROR ディスパッチは認可対象外にする
                    // （拒否すると 500 系が 401/403 に化ける）
                    .dispatcherTypeMatchers(DispatcherType.ERROR)
                    .permitAll()
                    // GET 系（閲覧）は公開のまま（#90 表）
                    .requestMatchers(HttpMethod.GET, "/**")
                    .permitAll()
                    // 認証の入り口は公開
                    .requestMatchers(
                        HttpMethod.POST,
                        "/api/v1/auth/signup",
                        "/api/v1/auth/login",
                        "/api/v1/auth/google",
                        "/api/v1/auth/refresh",
                    ).permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/v1/auth/logout")
                    .authenticated()
                    // レビュー投稿・更新は認証済みユーザー（一般ロール可）
                    .requestMatchers(HttpMethod.POST, "/api/v1/tourism/spots/*/reviews")
                    .authenticated()
                    .requestMatchers(HttpMethod.PUT, "/api/v1/tourism/spots/*/reviews/*")
                    .authenticated()
                    // 上記以外（GET 以外の未分類リクエスト全て）は運営ロール必須。
                    // ADMIN 専用の DELETE /api/v1/auth/users/{id} もここを通過し、
                    // メソッドセキュリティ（@PreAuthorize）で ADMIN に絞る。
                    .anyRequest()
                    .hasRole(Role.OPERATOR.name)
            }.exceptionHandling { handling ->
                handling
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler)
            }.oauth2ResourceServer { oauth2 ->
                oauth2
                    .jwt { jwt ->
                        jwt.jwtAuthenticationConverter(SupabaseJwtAuthenticationConverter())
                    }.authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler)
            }
        return http.build()
    }

    /**
     * ロール階層（#90）: ADMIN > OPERATOR > GENERAL。
     *
     * Bean として公開すると `authorizeHttpRequests` と `@PreAuthorize` の両方が参照する。
     */
    @Bean
    fun roleHierarchy(): RoleHierarchy =
        RoleHierarchyImpl
            .withDefaultRolePrefix()
            .role(Role.ADMIN.name)
            .implies(Role.OPERATOR.name)
            .role(Role.OPERATOR.name)
            .implies(Role.GENERAL.name)
            .build()

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

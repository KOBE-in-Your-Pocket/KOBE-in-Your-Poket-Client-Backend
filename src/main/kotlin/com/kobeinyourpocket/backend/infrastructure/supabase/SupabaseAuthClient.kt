package com.kobeinyourpocket.backend.infrastructure.supabase

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.kobeinyourpocket.backend.application.user.auth.AuthGateway
import com.kobeinyourpocket.backend.application.user.auth.AuthGatewayException
import com.kobeinyourpocket.backend.application.user.auth.AuthSession
import com.kobeinyourpocket.backend.domain.user.model.User
import org.springframework.http.MediaType
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import java.time.Duration

/**
 * Supabase GoTrue HTTP クライアント（#89-b）。
 *
 * Client → backend → 本クラス → SUPABASE_URL の /auth/v1 配下。
 */
@Component
class SupabaseAuthClient(
    private val props: SupabaseAuthProperties,
) : AuthGateway {
    private val restClient: RestClient =
        RestClient
            .builder()
            .baseUrl(props.url.trimEnd('/'))
            .defaultHeader("apikey", props.anonKey)
            .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .requestFactory(
                SimpleClientHttpRequestFactory().apply {
                    setConnectTimeout(Duration.ofSeconds(5))
                    setReadTimeout(Duration.ofSeconds(10))
                },
            ).build()

    /** Admin API（service_role キー）専用クライアント。ユーザー削除等の高権限操作に使用。 */
    private val adminRestClient: RestClient =
        RestClient
            .builder()
            .baseUrl(props.url.trimEnd('/'))
            .defaultHeader("apikey", props.serviceRoleKey)
            .defaultHeader("Authorization", "Bearer ${props.serviceRoleKey}")
            .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .requestFactory(
                SimpleClientHttpRequestFactory().apply {
                    setConnectTimeout(Duration.ofSeconds(5))
                    setReadTimeout(Duration.ofSeconds(10))
                },
            ).build()

    override fun signUp(
        email: String,
        password: String,
    ): AuthSession =
        postJson(
            path = "/auth/v1/signup",
            body = mapOf("email" to email, "password" to password),
        )

    override fun signInWithPassword(
        email: String,
        password: String,
    ): AuthSession =
        postJson(
            path = "/auth/v1/token?grant_type=password",
            body = mapOf("email" to email, "password" to password),
        )

    override fun refresh(refreshToken: String): AuthSession =
        postJson(
            path = "/auth/v1/token?grant_type=refresh_token",
            body = mapOf("refresh_token" to refreshToken),
        )

    override fun signOut(accessToken: String) {
        try {
            restClient
                .post()
                .uri("/auth/v1/logout")
                .header("Authorization", "Bearer $accessToken")
                .retrieve()
                .toBodilessEntity()
        } catch (ex: RestClientResponseException) {
            throw AuthGatewayException(
                status = ex.statusCode.value(),
                message = ex.responseBodyAsString.ifBlank { ex.message ?: "Supabase logout failed" },
                cause = ex,
            )
        }
    }

    override fun deleteUser(userId: User.Id) {
        try {
            adminRestClient
                .delete()
                .uri("/auth/v1/admin/users/${userId.value}")
                .retrieve()
                .toBodilessEntity()
        } catch (ex: RestClientResponseException) {
            throw AuthGatewayException(
                status = ex.statusCode.value(),
                message = ex.responseBodyAsString.ifBlank { ex.message ?: "Supabase user deletion failed" },
                cause = ex,
            )
        }
    }

    private fun postJson(
        path: String,
        body: Map<String, String>,
    ): AuthSession {
        val response =
            try {
                restClient
                    .post()
                    .uri(path)
                    .body(body)
                    .retrieve()
                    .body(GoTrueSessionResponse::class.java)
            } catch (ex: RestClientResponseException) {
                throw AuthGatewayException(
                    status = ex.statusCode.value(),
                    message = ex.responseBodyAsString.ifBlank { ex.message ?: "Supabase auth failed" },
                    cause = ex,
                )
            } ?: throw AuthGatewayException(status = 502, message = "Empty response from Supabase Auth")

        // Confirm email ON 時の再 signup や、メール送信レート制限直後などでは
        // 2xx でも user が空になることがある（列挙防止・制限レスポンスの取り違え防止）。
        val userId =
            response.user?.id
                ?: throw AuthGatewayException(
                    status = 502,
                    message =
                        "Supabase Auth response missing user.id " +
                            "(already registered, confirmation pending, or email rate limited). " +
                            "Confirm the email then POST /api/v1/auth/login, or wait and retry with a new +alias.",
                )
        return AuthSession(
            userId = User.Id.of(userId),
            accessToken = response.accessToken,
            refreshToken = response.refreshToken,
            expiresIn = response.expiresIn,
            tokenType = response.tokenType,
        )
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class GoTrueSessionResponse(
        @JsonProperty("access_token") val accessToken: String? = null,
        @JsonProperty("refresh_token") val refreshToken: String? = null,
        @JsonProperty("expires_in") val expiresIn: Long? = null,
        @JsonProperty("token_type") val tokenType: String? = null,
        val user: GoTrueUser? = null,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class GoTrueUser(
        val id: String? = null,
    )
}

package com.kobeinyourpocket.backend.infrastructure.supabase

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.kobeinyourpocket.backend.application.user.auth.AuthGateway
import com.kobeinyourpocket.backend.application.user.auth.AuthGatewayException
import com.kobeinyourpocket.backend.application.user.auth.AuthSession
import com.kobeinyourpocket.backend.domain.user.model.User
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException

/**
 * Supabase GoTrue HTTP クライアント（#89-b）。
 *
 * Client → backend → 本クラス → SUPABASE_URL の /auth/v1 配下。
 */
@Component
@EnableConfigurationProperties(SupabaseAuthProperties::class)
class SupabaseAuthClient(
    private val props: SupabaseAuthProperties,
) : AuthGateway {
    private val restClient: RestClient =
        RestClient
            .builder()
            .baseUrl(props.url.trimEnd('/'))
            .defaultHeader("apikey", props.anonKey)
            .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .build()

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
        require(props.url.isNotBlank() && props.anonKey.isNotBlank()) {
            "supabase.url / supabase.anon-key (SUPABASE_URL / SUPABASE_ANON_KEY) must be set"
        }
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
            )
        }
    }

    private fun postJson(
        path: String,
        body: Map<String, String>,
    ): AuthSession {
        require(props.url.isNotBlank() && props.anonKey.isNotBlank()) {
            "supabase.url / supabase.anon-key (SUPABASE_URL / SUPABASE_ANON_KEY) must be set"
        }
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
                )
            } ?: throw AuthGatewayException(status = 502, message = "Empty response from Supabase Auth")

        val userId =
            response.user?.id
                ?: throw AuthGatewayException(status = 502, message = "Supabase Auth response missing user.id")
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

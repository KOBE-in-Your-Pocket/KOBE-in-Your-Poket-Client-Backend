package com.kobeinyourpocket.backend.infrastructure.security

import com.kobeinyourpocket.backend.domain.user.vo.Role
import org.springframework.security.oauth2.jwt.Jwt
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class SupabaseJwtAuthenticationConverterTest {
    @Test
    fun `app_metadata role=operator を ROLE_OPERATOR にする`() {
        val jwt =
            jwtWithAppMetadata(
                mapOf(SupabaseJwtAuthenticationConverter.ROLE_CLAIM_KEY to Role.OPERATOR.claimValue),
            )

        val authorities = SupabaseJwtAuthenticationConverter.authoritiesFrom(jwt).map { it.authority }

        assertEquals(listOf(Role.OPERATOR.authority), authorities)
    }

    @Test
    fun `app_metadata 無しは ROLE_GENERAL`() {
        val jwt =
            Jwt
                .withTokenValue("token")
                .header("alg", "HS256")
                .subject("11111111-1111-1111-1111-111111111111")
                .issuedAt(Instant.parse("2026-07-14T00:00:00Z"))
                .expiresAt(Instant.parse("2026-07-14T01:00:00Z"))
                .build()

        val authorities = SupabaseJwtAuthenticationConverter.authoritiesFrom(jwt).map { it.authority }

        assertEquals(listOf(Role.GENERAL.authority), authorities)
    }

    private fun jwtWithAppMetadata(appMetadata: Map<String, Any>): Jwt =
        Jwt
            .withTokenValue("token")
            .header("alg", "HS256")
            .subject("11111111-1111-1111-1111-111111111111")
            .claim(SupabaseJwtAuthenticationConverter.APP_METADATA_CLAIM, appMetadata)
            .issuedAt(Instant.parse("2026-07-14T00:00:00Z"))
            .expiresAt(Instant.parse("2026-07-14T01:00:00Z"))
            .build()
}

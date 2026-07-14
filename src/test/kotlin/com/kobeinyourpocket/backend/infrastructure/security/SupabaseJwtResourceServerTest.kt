package com.kobeinyourpocket.backend.infrastructure.security

import com.kobeinyourpocket.backend.domain.user.vo.Role
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
import java.util.Date
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SupabaseJwtResourceServerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var jwtDecoder: JwtDecoder

    @Value("\${supabase.jwt.secret}")
    private lateinit var jwtSecret: String

    @Test
    fun `Bearer なしでも閲覧系は 200（permitAll）`() {
        mockMvc
            .perform(get("/api/ping"))
            .andExpect(status().isOk)
    }

    @Test
    fun `不正な Bearer は 401`() {
        mockMvc
            .perform(
                get("/api/ping")
                    .header("Authorization", "Bearer not-a-jwt"),
            ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `有効な JWT は検証でき operator ロールを authorities に載せる`() {
        val token = signHs256Jwt(roleClaim = Role.OPERATOR.claimValue)
        val jwt = jwtDecoder.decode(token)

        val authorities = SupabaseJwtAuthenticationConverter.authoritiesFrom(jwt).map { it.authority }
        assertTrue(Role.OPERATOR.authority in authorities)
        assertTrue(!jwt.subject.isNullOrBlank())
    }

    @Test
    fun `app_metadata に role が無い JWT は GENERAL になる`() {
        val token = signHs256Jwt(roleClaim = null)
        val jwt = jwtDecoder.decode(token)

        val authorities = SupabaseJwtAuthenticationConverter.authoritiesFrom(jwt).map { it.authority }
        assertEquals(listOf(Role.GENERAL.authority), authorities)
    }

    @Test
    fun `有効な Bearer 付きでも閲覧系は 200`() {
        val token = signHs256Jwt(roleClaim = Role.GENERAL.claimValue)
        mockMvc
            .perform(
                get("/api/ping")
                    .header("Authorization", "Bearer $token"),
            ).andExpect(status().isOk)
    }

    private fun signHs256Jwt(roleClaim: String?): String {
        val claimsBuilder =
            JWTClaimsSet
                .Builder()
                .subject(UUID.randomUUID().toString())
                .issueTime(Date.from(Instant.now()))
                .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
        if (roleClaim != null) {
            claimsBuilder.claim(
                SupabaseJwtAuthenticationConverter.APP_METADATA_CLAIM,
                mapOf(SupabaseJwtAuthenticationConverter.ROLE_CLAIM_KEY to roleClaim),
            )
        }
        val signed = SignedJWT(JWSHeader(JWSAlgorithm.HS256), claimsBuilder.build())
        signed.sign(MACSigner(jwtSecret.toByteArray(Charsets.UTF_8)))
        return signed.serialize()
    }
}

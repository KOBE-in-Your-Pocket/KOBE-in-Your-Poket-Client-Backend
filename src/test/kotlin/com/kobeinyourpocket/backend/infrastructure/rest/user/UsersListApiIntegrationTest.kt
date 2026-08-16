package com.kobeinyourpocket.backend.infrastructure.rest.user

import com.kobeinyourpocket.backend.domain.user.model.User
import com.kobeinyourpocket.backend.domain.user.repository.UserRepository
import com.kobeinyourpocket.backend.domain.user.vo.Role
import com.kobeinyourpocket.backend.infrastructure.security.SupabaseJwtAuthenticationConverter
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.Date
import java.util.UUID
import kotlin.test.Test

/**
 * `GET /api/v1/users` の契約テスト（#151）。
 *
 * 閲覧系は SecurityConfig で permitAll のため、この API を守るのはメソッドセキュリティ側。
 * 一般ロールに漏れないこと（要件 C-15 は運営業務）と、`data` + `meta` 契約を検証する。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UsersListApiIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    @Value("\${supabase.jwt.secret}")
    private lateinit var jwtSecret: String

    private val base = Instant.parse("2026-08-01T00:00:00Z")

    @Test
    fun `Bearer なしは 401`() {
        mockMvc
            .perform(get("/api/v1/users"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `一般ロールは 403`() {
        mockMvc
            .perform(get("/api/v1/users").header("Authorization", "Bearer ${jwt(Role.GENERAL)}"))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.status").value(403))
    }

    @Test
    fun `operator は一覧を取得できる`() {
        save("Alice", base)

        mockMvc
            .perform(get("/api/v1/users").header("Authorization", "Bearer ${jwt(Role.OPERATOR)}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].name").value("Alice"))
            .andExpect(jsonPath("$.data[0].id").isNotEmpty)
            .andExpect(jsonPath("$.data[0].createdAt").isNotEmpty)
            .andExpect(jsonPath("$.meta.page").value(0))
            .andExpect(jsonPath("$.meta.totalElements").value(1))
            .andExpect(jsonPath("$.meta.totalPages").value(1))
    }

    @Test
    fun `admin もロール階層で一覧を取得できる`() {
        save("Alice", base)

        mockMvc
            .perform(get("/api/v1/users").header("Authorization", "Bearer ${jwt(Role.ADMIN)}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].name").value("Alice"))
    }

    @Test
    fun `ロールは返さない`() {
        save("Alice", base)

        mockMvc
            .perform(get("/api/v1/users").header("Authorization", "Bearer ${jwt(Role.OPERATOR)}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].role").doesNotExist())
    }

    @Test
    fun `size で絞っても meta には総件数が入る`() {
        save("古い", base)
        save("新しい", base.plusSeconds(60))

        mockMvc
            .perform(
                get("/api/v1/users")
                    .param("size", "1")
                    .header("Authorization", "Bearer ${jwt(Role.OPERATOR)}"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].name").value("新しい"))
            .andExpect(jsonPath("$.meta.size").value(1))
            .andExpect(jsonPath("$.meta.totalElements").value(2))
            .andExpect(jsonPath("$.meta.totalPages").value(2))
    }

    @Test
    fun `上限を超える size は丸められて 200 を返す`() {
        mockMvc
            .perform(
                get("/api/v1/users")
                    .param("size", "10000")
                    .header("Authorization", "Bearer ${jwt(Role.OPERATOR)}"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.meta.size").value(200))
    }

    private fun save(
        name: String,
        createdAt: Instant,
    ) {
        userRepository.save(User.create(id = User.Id.of(UUID.randomUUID()), name = name, createdAt = createdAt))
    }

    private fun jwt(role: Role): String {
        val claims =
            JWTClaimsSet
                .Builder()
                .subject(UUID.randomUUID().toString())
                .issueTime(Date.from(Instant.now()))
                .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
                .claim(
                    SupabaseJwtAuthenticationConverter.APP_METADATA_CLAIM,
                    mapOf(SupabaseJwtAuthenticationConverter.ROLE_CLAIM_KEY to role.claimValue),
                ).build()
        val signed = SignedJWT(JWSHeader(JWSAlgorithm.HS256), claims)
        signed.sign(MACSigner(jwtSecret.toByteArray(Charsets.UTF_8)))
        return signed.serialize()
    }
}

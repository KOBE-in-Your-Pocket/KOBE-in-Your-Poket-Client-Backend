package com.kobeinyourpocket.backend.infrastructure.rest.user

import com.kobeinyourpocket.backend.domain.user.model.User
import com.kobeinyourpocket.backend.domain.user.repository.UserRepository
import com.kobeinyourpocket.backend.domain.user.vo.UserIcon
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
 * `GET /api/v1/users/me` の契約テスト（#91 / U-1）。
 *
 * 閲覧系オープンの例外として認証必須であること（未認証 401）と、
 * PublicUser 契約 `{ id, name, iconUrl }` を検証する。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UsersMeApiIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    @Value("\${supabase.jwt.secret}")
    private lateinit var jwtSecret: String

    @Test
    fun `Bearer なしは 401`() {
        mockMvc
            .perform(get("/api/v1/users/me"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `不正な Bearer は 401`() {
        mockMvc
            .perform(
                get("/api/v1/users/me")
                    .header("Authorization", "Bearer not-a-jwt"),
            ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `有効な JWT で自分の PublicUser を返す`() {
        val userId = User.Id.of(UUID.randomUUID())
        userRepository.save(
            User.create(id = userId, name = "Alice", icon = UserIcon.of("https://example.com/icon.png")),
        )

        mockMvc
            .perform(
                get("/api/v1/users/me")
                    .header("Authorization", "Bearer ${signHs256Jwt(userId)}"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(userId.value.toString()))
            .andExpect(jsonPath("$.name").value("Alice"))
            .andExpect(jsonPath("$.iconUrl").value("https://example.com/icon.png"))
    }

    @Test
    fun `有効な JWT でもプロフィール行が無ければ 404`() {
        val userId = User.Id.of(UUID.randomUUID())

        mockMvc
            .perform(
                get("/api/v1/users/me")
                    .header("Authorization", "Bearer ${signHs256Jwt(userId)}"),
            ).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.status").value(404))
    }

    private fun signHs256Jwt(userId: User.Id): String {
        val claims =
            JWTClaimsSet
                .Builder()
                .subject(userId.value.toString())
                .issueTime(Date.from(Instant.now()))
                .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
                .build()
        val signed = SignedJWT(JWSHeader(JWSAlgorithm.HS256), claims)
        signed.sign(MACSigner(jwtSecret.toByteArray(Charsets.UTF_8)))
        return signed.serialize()
    }
}

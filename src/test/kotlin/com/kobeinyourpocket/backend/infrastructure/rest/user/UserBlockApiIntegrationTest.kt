package com.kobeinyourpocket.backend.infrastructure.rest.user

import com.jayway.jsonpath.JsonPath
import com.kobeinyourpocket.backend.domain.user.model.User
import com.kobeinyourpocket.backend.domain.user.repository.UserRepository
import com.kobeinyourpocket.backend.domain.user.vo.Role
import com.kobeinyourpocket.backend.infrastructure.security.withRole
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * 利用者によるブロック API の統合テスト（#215）。
 * controller → application → JPA → DB を実 Bean で通し、ブロックがレビュー一覧に効くところまで見る。
 *
 * テスト環境は H2（application-test.yml、Hibernate create-drop / Flyway 無効）。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserBlockApiIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    @BeforeTest
    fun saveProfiles() {
        userRepository.save(User.create(User.Id.of(VIEWER), "Viewer"))
        userRepository.save(User.create(User.Id.of(TROLL), "Troll"))
        userRepository.save(User.create(User.Id.of(FRIEND), "Friend"))
    }

    private val spotBody =
        """
        {
          "genre": "landmark",
          "coordinates": { "latitude": 34.6826, "longitude": 135.1863 },
          "imageUrl": "https://example.com/kobe-port-tower.webp",
          "localizations": {
            "ja": { "name": "神戸ポートタワー", "categoryLabel": "ランドマーク", "description": "神戸のシンボル。",
                    "businessHours": "9:00-23:00", "address": "神戸市中央区波止場町5-5" },
            "en": { "name": "Kobe Port Tower", "categoryLabel": "Landmark", "description": "The symbol of Kobe.",
                    "businessHours": "9:00-23:00", "address": "5-5 Hatobacho, Chuo-ku, Kobe" },
            "zh": { "name": "神户港塔", "categoryLabel": "地标", "description": "神户的象征。",
                    "businessHours": "9:00-23:00", "address": "神户市中央区波止场町5-5" },
            "ko": { "name": "고베 포트 타워", "categoryLabel": "랜드마크", "description": "고베의 상징.",
                    "businessHours": "9:00-23:00", "address": "고베시 주오구 하토바초 5-5" }
          }
        }
        """.trimIndent()

    private fun registerSpot(): String {
        val result =
            mockMvc
                .perform(
                    post("/api/v1/tourism/spots")
                        .with(withRole(Role.OPERATOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(spotBody),
                ).andExpect(status().isCreated)
                .andReturn()
        return JsonPath.read(result.response.contentAsString, "$.id")
    }

    private fun postReview(
        spotId: String,
        author: String,
        comment: String,
    ) {
        mockMvc
            .perform(
                post("/api/v1/tourism/spots/$spotId/reviews")
                    .with(withRole(Role.GENERAL, author))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{ "rating": 3, "comment": "$comment", "author": { "name": "x" }, "language": "ja" }"""),
            ).andExpect(status().isCreated)
    }

    private fun listReviews(
        spotId: String,
        subject: String?,
    ): ResultActions {
        val request = get("/api/v1/tourism/spots/$spotId/reviews").param("lang", "ja")
        if (subject != null) request.with(withRole(Role.GENERAL, subject))
        return mockMvc.perform(request)
    }

    private fun block(
        target: String,
        subject: String? = VIEWER,
    ): ResultActions {
        val request = put("/api/v1/users/me/blocks/$target")
        if (subject != null) request.with(withRole(Role.GENERAL, subject))
        return mockMvc.perform(request)
    }

    private fun unblock(
        target: String,
        subject: String? = VIEWER,
    ): ResultActions {
        val request = delete("/api/v1/users/me/blocks/$target")
        if (subject != null) request.with(withRole(Role.GENERAL, subject))
        return mockMvc.perform(request)
    }

    // ---- ブロック・解除 ----

    @Test
    fun `ほかのユーザーをブロックすると 204 で一覧に載る`() {
        block(TROLL).andExpect(status().isNoContent)

        mockMvc
            .perform(get("/api/v1/users/me/blocks").with(withRole(Role.GENERAL, VIEWER)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(TROLL))
            .andExpect(jsonPath("$[0].name").value("Troll"))
            .andExpect(jsonPath("$[0].blockedAt").exists())
    }

    @Test
    fun `同じユーザーを 2 回ブロックしても 204 で 1 件のまま`() {
        block(TROLL).andExpect(status().isNoContent)
        block(TROLL).andExpect(status().isNoContent)

        mockMvc
            .perform(get("/api/v1/users/me/blocks").with(withRole(Role.GENERAL, VIEWER)))
            .andExpect(jsonPath("$.length()").value(1))
    }

    @Test
    fun `自分自身をブロックすると 400`() {
        block(VIEWER).andExpect(status().isBadRequest)
    }

    @Test
    fun `存在しないユーザーをブロックすると 404`() {
        block(UNKNOWN).andExpect(status().isNotFound)
    }

    @Test
    fun `userId の形式が不正なら 400`() {
        block("not-a-uuid").andExpect(status().isBadRequest)
    }

    @Test
    fun `未ログインはブロックも解除も一覧も 401`() {
        block(TROLL, subject = null).andExpect(status().isUnauthorized)
        unblock(TROLL, subject = null).andExpect(status().isUnauthorized)
        mockMvc.perform(get("/api/v1/users/me/blocks")).andExpect(status().isUnauthorized)
    }

    @Test
    fun `解除すると一覧から消える`() {
        block(TROLL).andExpect(status().isNoContent)

        unblock(TROLL).andExpect(status().isNoContent)

        mockMvc
            .perform(get("/api/v1/users/me/blocks").with(withRole(Role.GENERAL, VIEWER)))
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    fun `ブロックしていない相手の解除も 204`() {
        unblock(TROLL).andExpect(status().isNoContent)
    }

    @Test
    fun `ブロック一覧は本人の分だけを返す`() {
        block(TROLL, subject = FRIEND).andExpect(status().isNoContent)

        mockMvc
            .perform(get("/api/v1/users/me/blocks").with(withRole(Role.GENERAL, VIEWER)))
            .andExpect(jsonPath("$.length()").value(0))
    }

    // ---- レビュー一覧への反映 ----

    @Test
    fun `ブロックした投稿者のレビューは本人の一覧から消える`() {
        val spotId = registerSpot()
        postReview(spotId, TROLL, "荒らし")
        postReview(spotId, FRIEND, "良い場所")

        block(TROLL).andExpect(status().isNoContent)

        listReviews(spotId, VIEWER)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].comment").value("良い場所"))
    }

    @Test
    fun `ブロックはほかの閲覧者や未ログインの一覧には影響しない`() {
        val spotId = registerSpot()
        postReview(spotId, TROLL, "荒らし")
        postReview(spotId, FRIEND, "良い場所")

        block(TROLL).andExpect(status().isNoContent)

        listReviews(spotId, FRIEND).andExpect(jsonPath("$.length()").value(2))
        listReviews(spotId, subject = null).andExpect(jsonPath("$.length()").value(2))
    }

    @Test
    fun `解除するとレビューがまた表示される`() {
        val spotId = registerSpot()
        postReview(spotId, TROLL, "荒らし")
        block(TROLL).andExpect(status().isNoContent)

        unblock(TROLL).andExpect(status().isNoContent)

        listReviews(spotId, VIEWER).andExpect(jsonPath("$.length()").value(1))
    }

    private companion object {
        /** ブロックする側。 */
        const val VIEWER = "00000000-0000-0000-0000-0000000000b1"

        /** ブロックされる側。 */
        const val TROLL = "00000000-0000-0000-0000-0000000000b2"

        /** ブロックとは関係ない第三者。 */
        const val FRIEND = "00000000-0000-0000-0000-0000000000b3"

        /** プロフィール行が無いユーザー。 */
        const val UNKNOWN = "00000000-0000-0000-0000-0000000000b9"
    }
}

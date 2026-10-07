package com.kobeinyourpocket.backend.infrastructure.rest.report

import com.jayway.jsonpath.JsonPath
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import kotlin.test.Test

/**
 * レビュー通報 API の統合テスト（#147）。
 * controller → application → JPA → DB を実 Bean で通し、Client が受け取る status を検証する。
 *
 * テスト環境は H2（application-test.yml、Hibernate create-drop / Flyway 無効）。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ReviewReportApiIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

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

    /** [AUTHOR] としてレビューを投稿し、採番された reviewId を返す。 */
    private fun postReview(spotId: String): String {
        val result =
            mockMvc
                .perform(
                    post("/api/v1/tourism/spots/$spotId/reviews")
                        .with(withRole(Role.GENERAL, AUTHOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{ "rating": 1, "comment": "宣伝です", "author": { "name": "Alice" }, "language": "ja" }"""),
                ).andExpect(status().isCreated)
                .andReturn()
        return JsonPath.read(result.response.contentAsString, "$.id")
    }

    private fun report(
        spotId: String,
        reviewId: String,
        body: String = """{ "reason": "SPAM" }""",
        subject: String? = REPORTER,
    ): ResultActions {
        val request =
            post("/api/v1/tourism/spots/$spotId/reviews/$reviewId/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        if (subject != null) request.with(withRole(Role.GENERAL, subject))
        return mockMvc.perform(request)
    }

    @Test
    fun `他人のレビューを通報すると 201 と受付状態の通報を返す`() {
        val spotId = registerSpot()
        val reviewId = postReview(spotId)

        report(spotId, reviewId, """{ "reason": "HARASSMENT", "description": "投稿者を名指しで攻撃している" }""")
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.targetType").value("REVIEW"))
            .andExpect(jsonPath("$.targetId").value(reviewId))
            .andExpect(jsonPath("$.reason").value("HARASSMENT"))
            .andExpect(jsonPath("$.description").value("投稿者を名指しで攻撃している"))
            .andExpect(jsonPath("$.status").value("OPEN"))
            .andExpect(jsonPath("$.createdAt").exists())
    }

    @Test
    fun `その他は詳細付きなら 201`() {
        val spotId = registerSpot()
        val reviewId = postReview(spotId)

        report(spotId, reviewId, """{ "reason": "OTHER", "description": "別のスポットの話をしている" }""")
            .andExpect(status().isCreated)
    }

    @Test
    fun `未ログインは 401`() {
        val spotId = registerSpot()
        val reviewId = postReview(spotId)

        report(spotId, reviewId, subject = null).andExpect(status().isUnauthorized)
    }

    @Test
    fun `同じレビューを 2 回通報すると 409`() {
        val spotId = registerSpot()
        val reviewId = postReview(spotId)
        report(spotId, reviewId).andExpect(status().isCreated)

        report(spotId, reviewId, """{ "reason": "HATE" }""").andExpect(status().isConflict)
    }

    @Test
    fun `別のユーザーなら同じレビューを通報できる`() {
        val spotId = registerSpot()
        val reviewId = postReview(spotId)
        report(spotId, reviewId).andExpect(status().isCreated)

        report(spotId, reviewId, subject = OTHER_REPORTER).andExpect(status().isCreated)
    }

    @Test
    fun `自分のレビューを通報すると 400`() {
        val spotId = registerSpot()
        val reviewId = postReview(spotId)

        report(spotId, reviewId, subject = AUTHOR).andExpect(status().isBadRequest)
    }

    @Test
    fun `その他で詳細が空なら 400`() {
        val spotId = registerSpot()
        val reviewId = postReview(spotId)

        report(spotId, reviewId, """{ "reason": "OTHER" }""").andExpect(status().isBadRequest)
        report(spotId, reviewId, """{ "reason": "OTHER", "description": "  " }""").andExpect(status().isBadRequest)
    }

    @Test
    fun `未知の理由は 400`() {
        val spotId = registerSpot()
        val reviewId = postReview(spotId)

        report(spotId, reviewId, """{ "reason": "BORING" }""").andExpect(status().isBadRequest)
        report(spotId, reviewId, """{ "reason": "" }""").andExpect(status().isBadRequest)
    }

    @Test
    fun `詳細が 500 文字を超えると 400`() {
        val spotId = registerSpot()
        val reviewId = postReview(spotId)

        report(spotId, reviewId, """{ "reason": "SPAM", "description": "${"あ".repeat(501)}" }""")
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `存在しないレビューは 404`() {
        val spotId = registerSpot()

        report(spotId, "00000000-0000-0000-0000-00000000dead").andExpect(status().isNotFound)
    }

    @Test
    fun `別のスポットのパスで通報すると 404`() {
        val spotId = registerSpot()
        val reviewId = postReview(spotId)

        report("other-spot", reviewId).andExpect(status().isNotFound)
    }

    @Test
    fun `通報済みのレビューも運営は削除できる`() {
        val spotId = registerSpot()
        val reviewId = postReview(spotId)
        report(spotId, reviewId).andExpect(status().isCreated)

        mockMvc
            .perform(delete("/api/v1/tourism/reviews/$reviewId").with(withRole(Role.OPERATOR)))
            .andExpect(status().isNoContent)
    }

    private companion object {
        const val AUTHOR = "00000000-0000-0000-0000-0000000000b1"
        const val REPORTER = "00000000-0000-0000-0000-0000000000b2"
        const val OTHER_REPORTER = "00000000-0000-0000-0000-0000000000b3"
    }
}

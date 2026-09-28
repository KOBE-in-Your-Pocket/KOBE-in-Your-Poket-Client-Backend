package com.kobeinyourpocket.backend.infrastructure.rest.tourism

import com.jayway.jsonpath.JsonPath
import com.kobeinyourpocket.backend.domain.user.vo.Role
import com.kobeinyourpocket.backend.infrastructure.security.withRole
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import kotlin.test.Test

/**
 * ReviewController の統合テスト（§5.1 / #34）。
 * controller → application → JPA → DB を実 Bean で通し end-to-end で検証する。
 *
 * テスト環境は H2（application-test.yml、Hibernate create-drop / Flyway 無効）。
 * 各テストは [Transactional] でロールバックし相互に独立させる。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ReviewApiIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    private val spotBody =
        """
        {
          "genre": "landmark",
          "coordinates": { "latitude": 34.6826, "longitude": 135.1863 },
          "imageUrl": "https://example.com/kobe-port-tower.webp",
          "localizations": {
            "ja": {
              "name": "神戸ポートタワー",
              "categoryLabel": "ランドマーク",
              "description": "神戸のシンボル。",
              "businessHours": "9:00-23:00",
              "address": "神戸市中央区波止場町5-5"
            },
            "en": {
              "name": "Kobe Port Tower",
              "categoryLabel": "Landmark",
              "description": "The symbol of Kobe.",
              "businessHours": "9:00-23:00",
              "address": "5-5 Hatobacho, Chuo-ku, Kobe"
            },
            "zh": {
              "name": "神户港塔",
              "categoryLabel": "地标",
              "description": "神户的象征。",
              "businessHours": "9:00-23:00",
              "address": "神户市中央区波止场町5-5"
            },
            "ko": {
              "name": "고베 포트 타워",
              "categoryLabel": "랜드마크",
              "description": "고베의 상징.",
              "businessHours": "9:00-23:00",
              "address": "고베시 주오구 하토바초 5-5"
            }
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

    private fun reviewBody(
        rating: Int,
        comment: String,
        language: String = "ja",
    ) = """
        {
          "rating": $rating,
          "comment": "$comment",
          "author": { "name": "Alice" },
          "language": "$language"
        }
        """.trimIndent()

    /** レビューを投稿して採番された reviewId を返す。既定は [OWNER] の投稿。 */
    private fun postReview(
        spotId: String,
        subject: String = OWNER,
        rating: Int = 3,
        comment: String = "普通",
    ): String {
        val result =
            mockMvc
                .perform(
                    post("/api/v1/tourism/spots/$spotId/reviews")
                        .with(withRole(Role.GENERAL, subject))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewBody(rating, comment)),
                ).andExpect(status().isCreated)
                .andReturn()
        return JsonPath.read(result.response.contentAsString, "$.id")
    }

    @Test
    fun `POST でレビューを投稿し 201 とレビュー JSON を返す`() {
        val spotId = registerSpot()

        mockMvc
            .perform(
                post("/api/v1/tourism/spots/$spotId/reviews")
                    .with(withRole(Role.GENERAL))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(reviewBody(5, "最高の景色")),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.rating.value").value(5))
            .andExpect(jsonPath("$.comment").value("最高の景色"))
            .andExpect(jsonPath("$.author.name").value("Alice"))
            .andExpect(jsonPath("$.language").value("ja"))
            .andExpect(jsonPath("$.postedAt").exists())
    }

    @Test
    fun `GET lang=ja で該当言語のレビューのみ返す`() {
        val spotId = registerSpot()
        mockMvc.perform(
            post("/api/v1/tourism/spots/$spotId/reviews")
                .with(withRole(Role.GENERAL))
                .contentType(MediaType.APPLICATION_JSON)
                .content(reviewBody(4, "良い", "ja")),
        )
        mockMvc.perform(
            post("/api/v1/tourism/spots/$spotId/reviews")
                .with(withRole(Role.GENERAL))
                .contentType(MediaType.APPLICATION_JSON)
                .content(reviewBody(3, "Good", "en")),
        )

        mockMvc
            .perform(get("/api/v1/tourism/spots/$spotId/reviews?lang=ja"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].comment").value("良い"))
            .andExpect(jsonPath("$[0].language").value("ja"))
    }

    @Test
    fun `GET lang 未指定は en フォールバック`() {
        val spotId = registerSpot()
        mockMvc.perform(
            post("/api/v1/tourism/spots/$spotId/reviews")
                .with(withRole(Role.GENERAL))
                .contentType(MediaType.APPLICATION_JSON)
                .content(reviewBody(5, "Amazing", "en")),
        )

        mockMvc
            .perform(get("/api/v1/tourism/spots/$spotId/reviews"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].language").value("en"))
    }

    @Test
    fun `GET スポット未登録または未収録 spotId は空配列を返す`() {
        mockMvc
            .perform(get("/api/v1/tourism/spots/unknown-spot/reviews?lang=ja"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    fun `PUT で rating と comment を更新する`() {
        val spotId = registerSpot()
        val postResult =
            mockMvc
                .perform(
                    post("/api/v1/tourism/spots/$spotId/reviews")
                        .with(withRole(Role.GENERAL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewBody(3, "普通")),
                ).andReturn()
        val reviewId: String = JsonPath.read(postResult.response.contentAsString, "$.id")

        mockMvc
            .perform(
                put("/api/v1/tourism/spots/$spotId/reviews/$reviewId")
                    .with(withRole(Role.GENERAL))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{ "rating": 5, "comment": "やっぱり最高" }"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.rating.value").value(5))
            .andExpect(jsonPath("$.comment").value("やっぱり最高"))
    }

    @Test
    fun `レビュー投稿後に GET spots で rating が集計される`() {
        val spotId = registerSpot()
        mockMvc.perform(
            post("/api/v1/tourism/spots/$spotId/reviews")
                .with(withRole(Role.GENERAL))
                .contentType(MediaType.APPLICATION_JSON)
                .content(reviewBody(4, "good")),
        )
        mockMvc.perform(
            post("/api/v1/tourism/spots/$spotId/reviews")
                .with(withRole(Role.GENERAL))
                .contentType(MediaType.APPLICATION_JSON)
                .content(reviewBody(2, "meh")),
        )

        mockMvc
            .perform(get("/api/v1/tourism/spots?lang=ja"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].rating.value").value(3.0))
    }

    // ---- 投稿者本人による削除・編集（#86）----

    @Test
    fun `POST のレスポンスと GET 一覧に投稿者 id が入る`() {
        val spotId = registerSpot()
        postReview(spotId)

        // Client はこの id で「自分の投稿か」を判定し、削除・編集メニューを出す。
        mockMvc
            .perform(get("/api/v1/tourism/spots/$spotId/reviews?lang=ja"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].author.id").value(OWNER))
    }

    @Test
    fun `DELETE で自分のレビューを削除すると 204 になり一覧から消える`() {
        val spotId = registerSpot()
        val reviewId = postReview(spotId)

        mockMvc
            .perform(
                delete("/api/v1/tourism/spots/$spotId/reviews/$reviewId")
                    .with(withRole(Role.GENERAL, OWNER)),
            ).andExpect(status().isNoContent)

        mockMvc
            .perform(get("/api/v1/tourism/spots/$spotId/reviews?lang=ja"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    fun `DELETE は他人のレビューだと 403 でレビューは残る`() {
        val spotId = registerSpot()
        val reviewId = postReview(spotId)

        mockMvc
            .perform(
                delete("/api/v1/tourism/spots/$spotId/reviews/$reviewId")
                    .with(withRole(Role.GENERAL, OTHER_USER)),
            ).andExpect(status().isForbidden)
            .andExpect(jsonPath("$.status").value(403))

        mockMvc
            .perform(get("/api/v1/tourism/spots/$spotId/reviews?lang=ja"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
    }

    @Test
    fun `DELETE は未認証だと 401`() {
        val spotId = registerSpot()
        val reviewId = postReview(spotId)

        mockMvc
            .perform(delete("/api/v1/tourism/spots/$spotId/reviews/$reviewId"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `DELETE は存在しない reviewId だと 404`() {
        val spotId = registerSpot()

        mockMvc
            .perform(
                delete("/api/v1/tourism/spots/$spotId/reviews/00000000-0000-0000-0000-0000000000ee")
                    .with(withRole(Role.GENERAL, OWNER)),
            ).andExpect(status().isNotFound)
    }

    @Test
    fun `DELETE は reviewId が UUID 形式でなければ 400`() {
        val spotId = registerSpot()

        mockMvc
            .perform(
                delete("/api/v1/tourism/spots/$spotId/reviews/not-a-uuid")
                    .with(withRole(Role.GENERAL, OWNER)),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `PUT は他人のレビューだと 403 で内容が変わらない`() {
        val spotId = registerSpot()
        val reviewId = postReview(spotId, comment = "普通")

        mockMvc
            .perform(
                put("/api/v1/tourism/spots/$spotId/reviews/$reviewId")
                    .with(withRole(Role.GENERAL, OTHER_USER))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{ "rating": 1, "comment": "改竄" }"""),
            ).andExpect(status().isForbidden)

        mockMvc
            .perform(get("/api/v1/tourism/spots/$spotId/reviews?lang=ja"))
            .andExpect(jsonPath("$[0].comment").value("普通"))
    }

    @Test
    fun `PUT は reviewId が UUID 形式でなければ 400`() {
        val spotId = registerSpot()

        mockMvc
            .perform(
                put("/api/v1/tourism/spots/$spotId/reviews/not-a-uuid")
                    .with(withRole(Role.GENERAL, OWNER))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{ "rating": 3, "comment": "test" }"""),
            ).andExpect(status().isBadRequest)
    }

    private companion object {
        /** 投稿者本人の Supabase user id（JWT `sub`）。 */
        const val OWNER = "00000000-0000-0000-0000-0000000000a1"

        /** 投稿者ではない別ユーザー。 */
        const val OTHER_USER = "00000000-0000-0000-0000-0000000000a2"
    }
}

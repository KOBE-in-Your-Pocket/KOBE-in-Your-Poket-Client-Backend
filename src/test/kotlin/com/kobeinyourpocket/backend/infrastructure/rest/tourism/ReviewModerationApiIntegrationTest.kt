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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import kotlin.test.Test

/**
 * 運営向けレビュー管理 API の契約テスト（#165）。
 *
 * 公開の一覧（スポット別）と違い運営ロール限定であること、スポット名が要求言語で解決されること、
 * レビュー本文は投稿言語のまま返ることを end-to-end で検証する。
 *
 * テスト環境は H2（application-test.yml、Hibernate create-drop / Flyway 無効）。
 * 各テストは [Transactional] でロールバックし相互に独立させる。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ReviewModerationApiIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    private fun spotBody(
        ja: String,
        en: String,
    ) = """
        {
          "genre": "landmark",
          "coordinates": { "latitude": 34.6826, "longitude": 135.1863 },
          "imageUrl": "https://example.com/spot.webp",
          "localizations": {
            "ja": { "name": "$ja", "categoryLabel": "ランドマーク", "description": "説明",
                    "businessHours": "9:00-23:00", "address": "住所" },
            "en": { "name": "$en", "categoryLabel": "Landmark", "description": "Description",
                    "businessHours": "9:00-23:00", "address": "Address" },
            "zh": { "name": "中文名", "categoryLabel": "地标", "description": "说明",
                    "businessHours": "9:00-23:00", "address": "地址" },
            "ko": { "name": "한국어명", "categoryLabel": "랜드마크", "description": "설명",
                    "businessHours": "9:00-23:00", "address": "주소" }
          }
        }
        """.trimIndent()

    private fun registerSpot(
        ja: String = "神戸ポートタワー",
        en: String = "Kobe Port Tower",
    ): String {
        val result =
            mockMvc
                .perform(
                    post("/api/v1/tourism/spots")
                        .with(withRole(Role.OPERATOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(spotBody(ja, en)),
                ).andExpect(status().isCreated)
                .andReturn()
        return JsonPath.read(result.response.contentAsString, "$.id")
    }

    private fun postReview(
        spotId: String,
        rating: Int = 5,
        comment: String = "最高の景色",
        language: String = "ja",
        author: String = "Alice",
    ): String {
        val body =
            """
            { "rating": $rating, "comment": "$comment",
              "author": { "name": "$author" }, "language": "$language" }
            """.trimIndent()
        val result =
            mockMvc
                .perform(
                    post("/api/v1/tourism/spots/$spotId/reviews")
                        .with(withRole(Role.GENERAL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body),
                ).andExpect(status().isCreated)
                .andReturn()
        return JsonPath.read(result.response.contentAsString, "$.id")
    }

    @Test
    fun `GET は未認証だと 401`() {
        mockMvc
            .perform(get("/api/v1/tourism/reviews"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `GET は一般ロールだと 403`() {
        mockMvc
            .perform(get("/api/v1/tourism/reviews").with(withRole(Role.GENERAL)))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.status").value(403))
    }

    @Test
    fun `GET は運営ロールでスポット名つきの横断一覧を返す`() {
        val spotId = registerSpot()
        postReview(spotId, rating = 4, comment = "夜景が綺麗")

        mockMvc
            .perform(get("/api/v1/tourism/reviews?lang=ja").with(withRole(Role.OPERATOR)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].spotId").value(spotId))
            .andExpect(jsonPath("$.data[0].spotName").value("神戸ポートタワー"))
            .andExpect(jsonPath("$.data[0].rating.value").value(4))
            .andExpect(jsonPath("$.data[0].comment").value("夜景が綺麗"))
            .andExpect(jsonPath("$.data[0].author.name").value("Alice"))
            .andExpect(jsonPath("$.data[0].language").value("ja"))
            .andExpect(jsonPath("$.data[0].postedAt").exists())
            .andExpect(jsonPath("$.meta.page").value(0))
            .andExpect(jsonPath("$.meta.totalElements").value(1))
    }

    @Test
    fun `GET は admin でも取得できる（ロール階層）`() {
        mockMvc
            .perform(get("/api/v1/tourism/reviews").with(withRole(Role.ADMIN)))
            .andExpect(status().isOk)
    }

    @Test
    fun `GET は複数スポットのレビューを横断して返す`() {
        val portTower = registerSpot("神戸ポートタワー", "Kobe Port Tower")
        val arima = registerSpot("有馬温泉", "Arima Onsen")
        postReview(portTower, comment = "タワーのレビュー")
        postReview(arima, comment = "温泉のレビュー")

        mockMvc
            .perform(get("/api/v1/tourism/reviews?lang=ja").with(withRole(Role.OPERATOR)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.meta.totalElements").value(2))
    }

    @Test
    fun `GET の lang はスポット名の解決にのみ効き レビュー本文は投稿言語のまま返す`() {
        val spotId = registerSpot("神戸ポートタワー", "Kobe Port Tower")
        // 英語で投稿されたレビューを、運営が lang=ja で開く
        postReview(spotId, comment = "Great view", language = "en")

        mockMvc
            .perform(get("/api/v1/tourism/reviews?lang=ja").with(withRole(Role.OPERATOR)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].spotName").value("神戸ポートタワー"))
            .andExpect(jsonPath("$.data[0].comment").value("Great view"))
            .andExpect(jsonPath("$.data[0].language").value("en"))
    }

    @Test
    fun `GET の未対応言語コードは en へフォールバックする`() {
        val spotId = registerSpot("神戸ポートタワー", "Kobe Port Tower")
        postReview(spotId)

        mockMvc
            .perform(get("/api/v1/tourism/reviews?lang=fr").with(withRole(Role.OPERATOR)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].spotName").value("Kobe Port Tower"))
    }

    @Test
    fun `GET は size で絞っても meta に総件数を返す`() {
        val spotId = registerSpot()
        postReview(spotId, comment = "1件目")
        postReview(spotId, comment = "2件目")

        mockMvc
            .perform(get("/api/v1/tourism/reviews?size=1").with(withRole(Role.OPERATOR)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.meta.size").value(1))
            .andExpect(jsonPath("$.meta.totalElements").value(2))
            .andExpect(jsonPath("$.meta.totalPages").value(2))
    }

    @Test
    fun `GET の上限を超える size は丸められて 200 を返す`() {
        mockMvc
            .perform(get("/api/v1/tourism/reviews?size=10000").with(withRole(Role.OPERATOR)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.meta.size").value(200))
    }

    @Test
    fun `DELETE は未認証だと 401`() {
        mockMvc
            .perform(delete("/api/v1/tourism/reviews/${UUID.randomUUID()}"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `DELETE は一般ロールだと 403`() {
        val spotId = registerSpot()
        val reviewId = postReview(spotId)

        mockMvc
            .perform(delete("/api/v1/tourism/reviews/$reviewId").with(withRole(Role.GENERAL)))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `DELETE を運営ロールで実行すると 204 になり一覧から消える`() {
        val spotId = registerSpot()
        val reviewId = postReview(spotId)

        mockMvc
            .perform(delete("/api/v1/tourism/reviews/$reviewId").with(withRole(Role.OPERATOR)))
            .andExpect(status().isNoContent)

        mockMvc
            .perform(get("/api/v1/tourism/reviews").with(withRole(Role.OPERATOR)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.meta.totalElements").value(0))
    }

    @Test
    fun `DELETE した運営は投稿者本人でなくてよい`() {
        val spotId = registerSpot()
        val reviewId = postReview(spotId, author = "他人")

        // モデレーションは本人判定を行わない（#86 の本人削除とは別経路）
        mockMvc
            .perform(delete("/api/v1/tourism/reviews/$reviewId").with(withRole(Role.OPERATOR)))
            .andExpect(status().isNoContent)
    }

    @Test
    fun `DELETE が未登録なら 404 と統一エラー JSON を返す`() {
        mockMvc
            .perform(delete("/api/v1/tourism/reviews/${UUID.randomUUID()}").with(withRole(Role.OPERATOR)))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
    }

    @Test
    fun `DELETE は不正な UUID なら 400`() {
        mockMvc
            .perform(delete("/api/v1/tourism/reviews/not-a-uuid").with(withRole(Role.OPERATOR)))
            .andExpect(status().isBadRequest)
    }
}

package com.kobeinyourpocket.backend.infrastructure.rest.report

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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * 運営向け通報管理 API の統合テスト（#145）。
 * 通報の受付（#147）から一覧・対応・モデレーション削除との連動までを実 Bean と DB（H2）で通す。
 *
 * 口コミ A は 2 人、口コミ B は 1 人から通報される。未対応が多い A が先に並ぶ。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ReportModerationApiIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    private lateinit var spotId: String
    private lateinit var reviewA: String
    private lateinit var reviewB: String

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

    @BeforeTest
    fun setUp() {
        userRepository.save(User.create(id = User.Id.of(REPORTER_1), name = "Bob"))
        // REPORTER_2 はプロフィール行を持たない（名前は null で返る）。
        spotId = registerSpot()
        reviewA = postReview("格安ツアーはこちら")
        reviewB = postReview("最悪の場所")
        report(reviewA, REPORTER_1, """{ "reason": "SPAM" }""")
        report(reviewA, REPORTER_2, """{ "reason": "OTHER", "description": "外部サイトへの誘導" }""")
        report(reviewB, REPORTER_1, """{ "reason": "HARASSMENT" }""")
    }

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

    private fun postReview(comment: String): String {
        val result =
            mockMvc
                .perform(
                    post("/api/v1/tourism/spots/$spotId/reviews")
                        .with(withRole(Role.GENERAL, AUTHOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{ "rating": 1, "comment": "$comment", "author": { "name": "Alice" }, "language": "ja" }"""),
                ).andExpect(status().isCreated)
                .andReturn()
        return JsonPath.read(result.response.contentAsString, "$.id")
    }

    private fun report(
        reviewId: String,
        reporter: String,
        body: String,
    ) {
        mockMvc
            .perform(
                post("/api/v1/tourism/spots/$spotId/reviews/$reviewId/reports")
                    .with(withRole(Role.GENERAL, reporter))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body),
            ).andExpect(status().isCreated)
    }

    private fun list(query: String = ""): ResultActions =
        mockMvc.perform(get("/api/v1/reports/reviews?lang=ja$query").with(withRole(Role.OPERATOR, OPERATOR)))

    private fun handle(
        reviewId: String,
        body: String,
        role: Role = Role.OPERATOR,
    ): ResultActions =
        mockMvc.perform(
            patch("/api/v1/reports/reviews/$reviewId")
                .with(withRole(role, OPERATOR))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body),
        )

    @Test
    fun `一覧は運営ロール限定`() {
        mockMvc.perform(get("/api/v1/reports/reviews")).andExpect(status().isUnauthorized)
        mockMvc.perform(get("/api/v1/reports/reviews").with(withRole(Role.GENERAL))).andExpect(status().isForbidden)
        mockMvc.perform(get("/api/v1/reports/reviews").with(withRole(Role.ADMIN))).andExpect(status().isOk)
    }

    @Test
    fun `口コミごとにまとめ、未対応の通報が多い順に並べる`() {
        list()
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.meta.totalElements").value(2))
            .andExpect(jsonPath("$.data[0].reviewId").value(reviewA))
            .andExpect(jsonPath("$.data[0].reportCount").value(2))
            .andExpect(jsonPath("$.data[0].openCount").value(2))
            .andExpect(jsonPath("$.data[0].reasonCounts.SPAM").value(1))
            .andExpect(jsonPath("$.data[0].reasonCounts.OTHER").value(1))
            .andExpect(jsonPath("$.data[0].review.comment").value("格安ツアーはこちら"))
            .andExpect(jsonPath("$.data[0].review.spotName").value("神戸ポートタワー"))
            .andExpect(jsonPath("$.data[0].review.author.id").value(AUTHOR))
            .andExpect(jsonPath("$.data[0].review.author.name").value("Alice"))
            // 明細は新しい順。REPORTER_2 はプロフィール行が無いので名前は null。
            .andExpect(jsonPath("$.data[0].reports.length()").value(2))
            .andExpect(jsonPath("$.data[0].reports[0].reporter.id").value(REPORTER_2))
            .andExpect(jsonPath("$.data[0].reports[0].reporter.name").doesNotExist())
            .andExpect(jsonPath("$.data[0].reports[0].description").value("外部サイトへの誘導"))
            .andExpect(jsonPath("$.data[0].reports[1].reporter.name").value("Bob"))
            .andExpect(jsonPath("$.data[0].reports[1].status").value("OPEN"))
            .andExpect(jsonPath("$.data[1].reviewId").value(reviewB))
    }

    @Test
    fun `ページングは口コミ単位`() {
        list("&size=1&page=1")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].reviewId").value(reviewB))
            .andExpect(jsonPath("$.meta.totalElements").value(2))
            .andExpect(jsonPath("$.meta.totalPages").value(2))
    }

    @Test
    fun `却下すると未対応の一覧から消え、担当者と日時が残る`() {
        handle(reviewB, """{ "status": "DISMISSED" }""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.updatedCount").value(1))

        list("&status=OPEN")
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].reviewId").value(reviewA))
        list("&status=DISMISSED")
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].openCount").value(0))
            .andExpect(jsonPath("$.data[0].reports[0].status").value("DISMISSED"))
            .andExpect(jsonPath("$.data[0].reports[0].handledBy").value(OPERATOR))
            .andExpect(jsonPath("$.data[0].reports[0].handledAt").exists())
    }

    @Test
    fun `同じ口コミをもう一度閉じても 0 件で成功する`() {
        handle(reviewA, """{ "status": "RESOLVED" }""").andExpect(jsonPath("$.updatedCount").value(2))

        handle(reviewA, """{ "status": "DISMISSED" }""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.updatedCount").value(0))
    }

    @Test
    fun `対応状況の更新は不正な値・未対応への戻し・通報の無い口コミ・一般ロールを弾く`() {
        handle(reviewA, """{ "status": "OPEN" }""").andExpect(status().isBadRequest)
        handle(reviewA, """{ "status": "DONE" }""").andExpect(status().isBadRequest)
        handle(UUID.randomUUID().toString(), """{ "status": "RESOLVED" }""").andExpect(status().isNotFound)
        handle(reviewA, """{ "status": "RESOLVED" }""", role = Role.GENERAL).andExpect(status().isForbidden)
    }

    @Test
    fun `未知の status で一覧を絞ろうとすると 400`() {
        list("&status=DONE").andExpect(status().isBadRequest)
    }

    @Test
    fun `運営が口コミを削除すると未対応の通報は対応済みになり、通報は履歴として残る`() {
        mockMvc
            .perform(delete("/api/v1/tourism/reviews/$reviewA").with(withRole(Role.OPERATOR, OPERATOR)))
            .andExpect(status().isNoContent)

        list("&status=OPEN")
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].reviewId").value(reviewB))
        list("&status=RESOLVED")
            .andExpect(jsonPath("$.data[0].reviewId").value(reviewA))
            .andExpect(jsonPath("$.data[0].review").doesNotExist())
            .andExpect(jsonPath("$.data[0].reportCount").value(2))
            .andExpect(jsonPath("$.data[0].reports[0].status").value("RESOLVED"))
            .andExpect(jsonPath("$.data[0].reports[0].handledBy").value(OPERATOR))
    }

    private companion object {
        const val AUTHOR = "00000000-0000-0000-0000-0000000000c1"
        const val REPORTER_1 = "00000000-0000-0000-0000-0000000000c2"
        const val REPORTER_2 = "00000000-0000-0000-0000-0000000000c3"
        const val OPERATOR = "00000000-0000-0000-0000-0000000000c9"
    }
}

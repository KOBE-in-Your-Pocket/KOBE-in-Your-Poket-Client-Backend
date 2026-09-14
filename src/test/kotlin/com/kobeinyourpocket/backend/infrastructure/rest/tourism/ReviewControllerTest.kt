package com.kobeinyourpocket.backend.infrastructure.rest.tourism

import com.kobeinyourpocket.backend.application.tourism.command.DeleteOwnReviewService
import com.kobeinyourpocket.backend.application.tourism.command.PostReviewService
import com.kobeinyourpocket.backend.application.tourism.command.UpdateReviewService
import com.kobeinyourpocket.backend.application.tourism.query.ListReviewsService
import com.kobeinyourpocket.backend.application.tourism.query.ReviewView
import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewAuthorId
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewId
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId
import com.kobeinyourpocket.backend.infrastructure.rest.common.GlobalExceptionHandler
import org.mockito.BDDMockito.given
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
import java.util.UUID
import kotlin.test.Test

/**
 * ReviewController の GET と入力バリデーションのスライステスト。
 *
 * 書き込み系（POST / PUT / DELETE）の正常系と 403 は [ReviewApiIntegrationTest] が持つ。
 * このスライスは `addFilters = false` で Security を外しており、`@AuthenticationPrincipal`
 * が解決されない（#86 で投稿者 id を JWT から取るようにしたため）。認証が絡む経路は
 * 実フィルタを通す統合テストで見るのが正しい。
 */
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(ReviewController::class)
@Import(GlobalExceptionHandler::class)
class ReviewControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var postReviewService: PostReviewService

    @MockitoBean
    private lateinit var listReviewsService: ListReviewsService

    @MockitoBean
    private lateinit var updateReviewService: UpdateReviewService

    @MockitoBean
    private lateinit var deleteOwnReviewService: DeleteOwnReviewService

    private val spotId = SpotId.of("kobe-port-tower")
    private val reviewId = ReviewId.of(UUID.fromString("00000000-0000-0000-0000-000000000001"))
    private val now = Instant.parse("2025-11-03T10:00:00Z")

    /** リクエストの JWT `sub`。投稿者 id としてサービスに渡る（#86）。 */
    private val requesterId = ReviewAuthorId.of("11111111-1111-1111-1111-111111111111")

    private val reviewView =
        ReviewView(
            id = reviewId.toString(),
            spotId = spotId.value,
            rating = 4,
            comment = "素晴らしい",
            authorName = "Alice",
            authorIconUrl = "https://example.com/alice.png",
            authorUserId = requesterId.toString(),
            createdAt = now,
            language = "ja",
        )

    @Test
    fun `GET lang=ja でレビュー一覧を返す`() {
        given(listReviewsService.listReviews(spotId, Language.JA)).willReturn(listOf(reviewView))

        mockMvc
            .perform(get("/api/v1/tourism/spots/kobe-port-tower/reviews?lang=ja"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(reviewId.toString()))
            .andExpect(jsonPath("$[0].rating.value").value(4))
            .andExpect(jsonPath("$[0].comment").value("素晴らしい"))
            .andExpect(jsonPath("$[0].author.name").value("Alice"))
            .andExpect(jsonPath("$[0].author.iconUrl").value("https://example.com/alice.png"))
            .andExpect(jsonPath("$[0].language").value("ja"))

        verify(listReviewsService).listReviews(spotId, Language.JA)
    }

    @Test
    fun `GET lang 未指定は en フォールバック`() {
        given(listReviewsService.listReviews(spotId, Language.EN)).willReturn(emptyList())

        mockMvc
            .perform(get("/api/v1/tourism/spots/kobe-port-tower/reviews"))
            .andExpect(status().isOk)

        verify(listReviewsService).listReviews(spotId, Language.EN)
    }

    @Test
    fun `POST rating が範囲外なら 400`() {
        mockMvc
            .perform(
                post("/api/v1/tourism/spots/kobe-port-tower/reviews")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "rating": 6,
                          "comment": "test",
                          "author": { "name": "Alice" },
                          "language": "ja"
                        }
                        """.trimIndent(),
                    ),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.violations[0].field").value("rating"))
    }

    @Test
    fun `POST comment が空なら 400`() {
        mockMvc
            .perform(
                post("/api/v1/tourism/spots/kobe-port-tower/reviews")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "rating": 3,
                          "comment": "",
                          "author": { "name": "Alice" },
                          "language": "ja"
                        }
                        """.trimIndent(),
                    ),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.violations[0].field").value("comment"))
    }
}

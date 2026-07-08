package com.kobeinyourpocket.backend.infrastructure.rest.tourism

import com.kobeinyourpocket.backend.application.tourism.command.PostReviewService
import com.kobeinyourpocket.backend.application.tourism.command.UpdateReviewService
import com.kobeinyourpocket.backend.application.tourism.query.ListReviewsService
import com.kobeinyourpocket.backend.application.tourism.query.ReviewView
import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.domain.tourism.review.model.Review
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewAuthor
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewId
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewRating
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId
import com.kobeinyourpocket.backend.infrastructure.rest.common.GlobalExceptionHandler
import org.mockito.BDDMockito.given
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
import java.util.UUID
import kotlin.test.Test

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

    private val spotId = SpotId.of("kobe-port-tower")
    private val reviewId = ReviewId.of(UUID.fromString("00000000-0000-0000-0000-000000000001"))
    private val now = Instant.parse("2025-11-03T10:00:00Z")

    private val reviewView =
        ReviewView(
            id = reviewId.toString(),
            spotId = spotId.value,
            rating = 4,
            comment = "素晴らしい",
            authorName = "Alice",
            authorIconUrl = "https://example.com/alice.png",
            createdAt = now,
            language = "ja",
        )

    private val savedReview =
        Review(
            id = reviewId,
            spotId = spotId,
            rating = ReviewRating.of(4),
            comment = "素晴らしい",
            author = ReviewAuthor(name = "Alice", iconUrl = "https://example.com/alice.png"),
            createdAt = now,
            language = Language.JA,
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
    fun `POST でレビューを投稿し 201 を返す`() {
        given(
            postReviewService.postReview(
                spotId = spotId,
                rating = ReviewRating.of(4),
                comment = "素晴らしい",
                authorName = "Alice",
                language = Language.JA,
            ),
        ).willReturn(savedReview)

        mockMvc
            .perform(
                post("/api/v1/tourism/spots/kobe-port-tower/reviews")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "rating": 4,
                          "comment": "素晴らしい",
                          "author": { "name": "Alice", "iconUrl": "https://example.com/alice.png" },
                          "language": "ja"
                        }
                        """.trimIndent(),
                    ),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(reviewId.toString()))
            .andExpect(jsonPath("$.rating.value").value(4))
            .andExpect(jsonPath("$.comment").value("素晴らしい"))
            .andExpect(jsonPath("$.author.name").value("Alice"))
            .andExpect(jsonPath("$.language").value("ja"))
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

    @Test
    fun `PUT で rating と comment を更新する`() {
        given(
            updateReviewService.updateReview(
                reviewId = reviewId,
                rating = ReviewRating.of(5),
                comment = "最高でした",
            ),
        ).willReturn(savedReview.copy(rating = ReviewRating.of(5), comment = "最高でした"))

        mockMvc
            .perform(
                put("/api/v1/tourism/spots/kobe-port-tower/reviews/$reviewId")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{ "rating": 5, "comment": "最高でした" }"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.rating.value").value(5))
            .andExpect(jsonPath("$.comment").value("最高でした"))

        verify(updateReviewService).updateReview(reviewId, ReviewRating.of(5), "最高でした")
    }

    @Test
    fun `PUT reviewId が UUID 形式でなければ 400`() {
        mockMvc
            .perform(
                put("/api/v1/tourism/spots/kobe-port-tower/reviews/not-a-uuid")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{ "rating": 3, "comment": "test" }"""),
            ).andExpect(status().isBadRequest)
    }
}

package com.kobeinyourpocket.backend.application.tourism.command

import com.kobeinyourpocket.backend.domain.tourism.localization.Language
import com.kobeinyourpocket.backend.domain.tourism.review.model.Review
import com.kobeinyourpocket.backend.domain.tourism.review.repository.ReviewRepository
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewAuthor
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewId
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewRating
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UpdateReviewServiceTest {
    private val repository = mockk<ReviewRepository>()
    private val service = UpdateReviewService(repository)

    private val reviewId = ReviewId.of("00000000-0000-0000-0000-000000000001")
    private val existing =
        Review(
            id = reviewId,
            spotId = SpotId.of("kobe-port-tower"),
            rating = ReviewRating.of(3),
            comment = "普通でした",
            author = ReviewAuthor(name = "Alice"),
            createdAt = Instant.parse("2025-11-03T10:00:00Z"),
            language = Language.JA,
        )

    @Test
    fun `updateReview は rating と comment を差し替えて保存し返す`() {
        val saved = slot<Review>()
        every { repository.findById(reviewId) } returns existing
        every { repository.save(capture(saved)) } answers { saved.captured }

        val result = service.updateReview(reviewId, ReviewRating.of(5), "最高でした")

        assertEquals(5, result.rating.value)
        assertEquals("最高でした", result.comment)
        assertEquals(existing.id, result.id)
        assertEquals(existing.author, result.author)
        verify(exactly = 1) { repository.save(saved.captured) }
    }

    @Test
    fun `存在しない reviewId を渡すと error をスローする`() {
        every { repository.findById(reviewId) } returns null

        assertFailsWith<IllegalStateException> {
            service.updateReview(reviewId, ReviewRating.of(4), "更新")
        }
    }
}

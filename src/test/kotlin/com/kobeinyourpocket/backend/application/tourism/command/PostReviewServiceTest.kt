package com.kobeinyourpocket.backend.application.tourism.command

import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.domain.tourism.review.model.Review
import com.kobeinyourpocket.backend.domain.tourism.review.repository.ReviewRepository
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewAuthorId
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewRating
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PostReviewServiceTest {
    private val repository = mockk<ReviewRepository>()
    private val service = PostReviewService(repository)

    private val spotId = SpotId.of("kobe-port-tower")
    private val authorUserId = ReviewAuthorId.of("11111111-1111-1111-1111-111111111111")

    @Test
    fun `postReview は Review を採番して保存し返す`() {
        val saved = slot<Review>()
        every { repository.save(capture(saved)) } answers { saved.captured }

        val result =
            service.postReview(
                spotId = spotId,
                rating = ReviewRating.of(4),
                comment = "素晴らしい景色",
                authorName = "Alice",
                authorUserId = authorUserId,
                language = Language.JA,
            )

        assertEquals(spotId, result.spotId)
        assertEquals(4, result.rating.value)
        assertEquals("素晴らしい景色", result.comment)
        assertEquals("Alice", result.author.name)
        assertNull(result.author.iconUrl)
        assertEquals(Language.JA, result.language)
        verify(exactly = 1) { repository.save(saved.captured) }
    }

    @Test
    fun `postReview は投稿者の user id を記録する`() {
        val saved = slot<Review>()
        every { repository.save(capture(saved)) } answers { saved.captured }

        val result =
            service.postReview(
                spotId = spotId,
                rating = ReviewRating.of(4),
                comment = "素晴らしい景色",
                authorName = "Alice",
                authorUserId = authorUserId,
                language = Language.JA,
            )

        // 本人削除・本人編集（#86）はこの id だけを根拠に判定する。
        assertEquals(authorUserId, result.author.userId)
        assertEquals(authorUserId, saved.captured.author.userId)
    }

    @Test
    fun `postReview は ReviewId をサーバー側で採番する`() {
        val first = slot<Review>()
        val second = slot<Review>()
        every { repository.save(capture(first)) } answers { first.captured }
        every { repository.save(capture(second)) } answers { second.captured }

        val r1 =
            service.postReview(spotId, ReviewRating.of(5), "Good", "Bob", authorUserId, Language.EN)
        val r2 =
            service.postReview(spotId, ReviewRating.of(3), "Okay", "Carol", authorUserId, Language.EN)

        assert(r1.id != r2.id) { "採番した ID は毎回異なるはず" }
    }
}

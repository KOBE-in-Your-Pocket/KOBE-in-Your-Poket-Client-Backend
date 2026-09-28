package com.kobeinyourpocket.backend.application.tourism.command

import com.kobeinyourpocket.backend.application.tourism.ReviewNotFoundException
import com.kobeinyourpocket.backend.application.tourism.ReviewNotOwnedException
import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.domain.tourism.review.model.Review
import com.kobeinyourpocket.backend.domain.tourism.review.repository.ReviewRepository
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewAuthor
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewAuthorId
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewId
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewRating
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertFailsWith

class DeleteOwnReviewServiceTest {
    private val repository = mockk<ReviewRepository>(relaxUnitFun = true)
    private val service = DeleteOwnReviewService(repository)

    private val reviewId = ReviewId.of("00000000-0000-0000-0000-000000000001")
    private val author = ReviewAuthorId.of("11111111-1111-1111-1111-111111111111")
    private val otherUser = ReviewAuthorId.of("22222222-2222-2222-2222-222222222222")

    private val existing =
        Review(
            id = reviewId,
            spotId = SpotId.of("kobe-port-tower"),
            rating = ReviewRating.of(3),
            comment = "普通でした",
            author = ReviewAuthor(name = "Alice", userId = author),
            createdAt = Instant.parse("2025-11-03T10:00:00Z"),
            language = Language.JA,
        )

    @Test
    fun `投稿者本人は自分のレビューを削除できる`() {
        every { repository.findById(reviewId) } returns existing

        service.execute(reviewId, author)

        verify(exactly = 1) { repository.deleteById(reviewId) }
    }

    @Test
    fun `他人のレビューは削除できない`() {
        every { repository.findById(reviewId) } returns existing

        assertFailsWith<ReviewNotOwnedException> {
            service.execute(reviewId, otherUser)
        }

        verify(exactly = 0) { repository.deleteById(any()) }
    }

    @Test
    fun `存在しない reviewId は ReviewNotFoundException`() {
        every { repository.findById(reviewId) } returns null

        assertFailsWith<ReviewNotFoundException> {
            service.execute(reviewId, author)
        }

        verify(exactly = 0) { repository.deleteById(any()) }
    }

    @Test
    fun `投稿者不明の古いレビューは本人削除の対象外`() {
        // V17 以前の投稿。運営のモデレーション削除でのみ消せる。
        every { repository.findById(reviewId) } returns
            existing.copy(author = ReviewAuthor(name = "Alice", userId = null))

        assertFailsWith<ReviewNotOwnedException> {
            service.execute(reviewId, author)
        }

        verify(exactly = 0) { repository.deleteById(any()) }
    }
}

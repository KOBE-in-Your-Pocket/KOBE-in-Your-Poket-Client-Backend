package com.kobeinyourpocket.backend.domain.tourism

import com.kobeinyourpocket.backend.domain.tourism.localization.Language
import com.kobeinyourpocket.backend.domain.tourism.review.model.Review
import com.kobeinyourpocket.backend.domain.tourism.review.repository.ReviewRepository
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewAuthor
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewId
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewRating
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

/** ReviewRepository write port の契約を Fake で検証する。 */
class ReviewRepositoryPortTest {
    private class FakeReviewRepository : ReviewRepository {
        private val store = linkedMapOf<ReviewId, Review>()

        override fun save(review: Review): Review {
            store[review.id] = review
            return review
        }

        override fun findById(id: ReviewId): Review? = store[id]

        fun get(id: ReviewId): Review? = store[id]
    }

    @Test
    fun `save した Review を取得できる`() {
        val repository = FakeReviewRepository()
        val review =
            Review.create(
                spotId = SpotId.of("kobe-port-tower"),
                rating = ReviewRating.of(5),
                comment = "Great spot!",
                author = ReviewAuthor(name = "Alice"),
                language = Language.EN,
                createdAt = Instant.parse("2025-11-03T10:24:00Z"),
            )

        repository.save(review)

        assertEquals(review, repository.get(review.id))
    }

    @Test
    fun `異なる spotId を持つ Review をそれぞれ保存できる`() {
        val repository = FakeReviewRepository()
        val review1 =
            Review.create(
                spotId = SpotId.of("kobe-port-tower"),
                rating = ReviewRating.of(4),
                comment = "Nice view",
                author = ReviewAuthor(name = "Bob"),
                language = Language.EN,
                createdAt = Instant.parse("2025-11-03T10:00:00Z"),
            )
        val review2 =
            Review.create(
                spotId = SpotId.of("meriken-park"),
                rating = ReviewRating.of(3),
                comment = "良い場所です",
                author = ReviewAuthor(name = "Carol"),
                language = Language.JA,
                createdAt = Instant.parse("2025-11-03T11:00:00Z"),
            )

        repository.save(review1)
        repository.save(review2)

        assertEquals(review1, repository.get(review1.id))
        assertEquals(review2, repository.get(review2.id))
    }
}

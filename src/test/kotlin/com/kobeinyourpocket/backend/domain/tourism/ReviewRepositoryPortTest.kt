package com.kobeinyourpocket.backend.domain.tourism

import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.domain.tourism.review.model.Review
import com.kobeinyourpocket.backend.domain.tourism.review.repository.ReviewRepository
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewAuthor
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewAuthorId
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewId
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewRating
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** ReviewRepository write port の契約を Fake で検証する。 */
class ReviewRepositoryPortTest {
    private class FakeReviewRepository : ReviewRepository {
        private val store = linkedMapOf<ReviewId, Review>()

        override fun save(review: Review): Review {
            store[review.id] = review
            return review
        }

        override fun findById(id: ReviewId): Review? = store[id]

        override fun existsById(id: ReviewId): Boolean = store.containsKey(id)

        override fun deleteById(id: ReviewId) {
            store.remove(id)
        }

        override fun deleteByAuthorId(authorId: ReviewAuthorId): Int {
            val targets = store.values.filter { it.author.isOwnedBy(authorId) }.map { it.id }
            targets.forEach { store.remove(it) }
            return targets.size
        }

        fun get(id: ReviewId): Review? = store[id]
    }

    private fun review(
        comment: String = "Great spot!",
        author: ReviewAuthor = ReviewAuthor(name = "Alice"),
    ): Review =
        Review.create(
            spotId = SpotId.of("kobe-port-tower"),
            rating = ReviewRating.of(5),
            comment = comment,
            author = author,
            language = Language.EN,
            createdAt = Instant.parse("2025-11-03T10:24:00Z"),
        )

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

    @Test
    fun `existsById は save 済みかどうかを返す`() {
        val repository = FakeReviewRepository()
        val saved = review()
        repository.save(saved)

        assertTrue(repository.existsById(saved.id))
        assertFalse(repository.existsById(review("別のレビュー").id))
    }

    @Test
    fun `deleteById した Review は取得できなくなる`() {
        val repository = FakeReviewRepository()
        val saved = review()
        repository.save(saved)

        repository.deleteById(saved.id)

        assertNull(repository.get(saved.id))
        assertFalse(repository.existsById(saved.id))
    }

    @Test
    fun `deleteByAuthorId は投稿者本人のレビューだけを消し、件数を返す`() {
        val repository = FakeReviewRepository()
        val alice = ReviewAuthorId.of(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"))
        val bob = ReviewAuthorId.of(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"))
        val aliceReview = repository.save(review(author = ReviewAuthor(name = "Alice", userId = alice)))
        val bobReview = repository.save(review(author = ReviewAuthor(name = "Bob", userId = bob)))
        // 投稿者不明（V17 以前）の投稿は誰の退会でも消えない。
        val legacyReview = repository.save(review(author = ReviewAuthor(name = "Legacy")))

        assertEquals(1, repository.deleteByAuthorId(alice))

        assertNull(repository.get(aliceReview.id))
        assertEquals(bobReview, repository.get(bobReview.id))
        assertEquals(legacyReview, repository.get(legacyReview.id))
    }

    @Test
    fun `deleteByAuthorId は該当が無ければ 0 を返す`() {
        val repository = FakeReviewRepository()
        repository.save(review())

        val stranger = ReviewAuthorId.of(UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"))

        assertEquals(0, repository.deleteByAuthorId(stranger))
    }
}

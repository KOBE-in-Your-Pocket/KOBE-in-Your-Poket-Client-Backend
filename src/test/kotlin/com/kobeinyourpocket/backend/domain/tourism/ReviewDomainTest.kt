package com.kobeinyourpocket.backend.domain.tourism

import com.kobeinyourpocket.backend.domain.tourism.aggregate.Review
import com.kobeinyourpocket.backend.domain.tourism.vo.Language
import com.kobeinyourpocket.backend.domain.tourism.vo.ReviewAuthor
import com.kobeinyourpocket.backend.domain.tourism.vo.ReviewId
import com.kobeinyourpocket.backend.domain.tourism.vo.ReviewRating
import com.kobeinyourpocket.backend.domain.tourism.vo.SpotId
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class ReviewIdTest {
    @Test
    fun `UUID 文字列から復元できる`() {
        val uuid = UUID.randomUUID()
        val id = ReviewId.of(uuid.toString())

        assertEquals(uuid, id.value)
    }

    @Test
    fun `UUID インスタンスから生成できる`() {
        val uuid = UUID.randomUUID()
        val id = ReviewId.of(uuid)

        assertEquals(uuid, id.value)
    }

    @Test
    fun `generate は呼び出しごとに異なる ID を返す`() {
        assertNotEquals(ReviewId.generate(), ReviewId.generate())
    }

    @Test
    fun `不正な UUID 文字列は拒否する`() {
        assertFailsWith<IllegalArgumentException> {
            ReviewId.of("not-a-uuid")
        }
    }
}

class ReviewRatingTest {
    @Test
    fun `1 から 5 の整数値で生成できる`() {
        for (v in 1..5) {
            assertEquals(v, ReviewRating.of(v).value)
        }
    }

    @Test
    fun `0 以下は拒否する`() {
        assertFailsWith<IllegalArgumentException> {
            ReviewRating.of(0)
        }
    }

    @Test
    fun `6 以上は拒否する`() {
        assertFailsWith<IllegalArgumentException> {
            ReviewRating.of(6)
        }
    }
}

class ReviewAuthorTest {
    @Test
    fun `name と iconUrl を指定して生成できる`() {
        val author = ReviewAuthor(name = "Alice", iconUrl = "https://example.com/alice.png")

        assertEquals("Alice", author.name)
        assertEquals("https://example.com/alice.png", author.iconUrl)
    }

    @Test
    fun `iconUrl は省略できる`() {
        val author = ReviewAuthor(name = "Alice")

        assertEquals(null, author.iconUrl)
    }

    @Test
    fun `name が空白のみなら拒否する`() {
        assertFailsWith<IllegalArgumentException> {
            ReviewAuthor(name = "   ")
        }
    }

    @Test
    fun `name が上限 100 文字を超えたら拒否する`() {
        assertFailsWith<IllegalArgumentException> {
            ReviewAuthor(name = "a".repeat(ReviewAuthor.MAX_NAME_LENGTH + 1))
        }
    }

    @Test
    fun `name が上限ちょうどなら許可する`() {
        val author = ReviewAuthor(name = "a".repeat(ReviewAuthor.MAX_NAME_LENGTH))

        assertEquals(ReviewAuthor.MAX_NAME_LENGTH, author.name.length)
    }
}

class ReviewTest {
    private val spotId = SpotId.of("kobe-port-tower")
    private val rating = ReviewRating.of(5)
    private val author = ReviewAuthor(name = "Alice", iconUrl = "https://example.com/alice.png")
    private val now = Instant.parse("2025-11-03T10:24:00Z")

    @Test
    fun `有効な入力でレビューを生成できる`() {
        val review =
            Review.create(
                spotId = spotId,
                rating = rating,
                comment = "Great spot!",
                author = author,
                language = Language.EN,
                createdAt = now,
            )

        assertEquals(spotId, review.spotId)
        assertEquals(rating, review.rating)
        assertEquals("Great spot!", review.comment)
        assertEquals(author, review.author)
        assertEquals(now, review.createdAt)
        assertEquals(Language.EN, review.language)
    }

    @Test
    fun `create は呼び出しごとに一意な ID を採番する`() {
        val r1 =
            Review.create(spotId = spotId, rating = rating, comment = "A", author = ReviewAuthor(name = "A"), language = Language.JA)
        val r2 =
            Review.create(spotId = spotId, rating = rating, comment = "B", author = ReviewAuthor(name = "B"), language = Language.JA)

        assertNotEquals(r1.id, r2.id)
    }

    @Test
    fun `comment が空白のみなら拒否する`() {
        assertFailsWith<IllegalArgumentException> {
            Review.create(spotId = spotId, rating = rating, comment = "   ", author = author, language = Language.JA)
        }
    }

    @Test
    fun `comment が上限 1000 文字を超えたら拒否する`() {
        val tooLong = "a".repeat(Review.MAX_COMMENT_LENGTH + 1)

        assertFailsWith<IllegalArgumentException> {
            Review.create(spotId = spotId, rating = rating, comment = tooLong, author = author, language = Language.JA)
        }
    }

    @Test
    fun `comment が上限ちょうどなら許可する`() {
        val boundary = "a".repeat(Review.MAX_COMMENT_LENGTH)
        val review =
            Review.create(
                spotId = spotId,
                rating = rating,
                comment = boundary,
                author = author,
                language = Language.JA,
            )

        assertEquals(Review.MAX_COMMENT_LENGTH, review.comment.length)
    }
}

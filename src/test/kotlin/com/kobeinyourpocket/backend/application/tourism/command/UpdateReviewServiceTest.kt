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
import com.kobeinyourpocket.backend.domain.user.model.User
import com.kobeinyourpocket.backend.domain.user.repository.UserRepository
import com.kobeinyourpocket.backend.domain.user.vo.UserIcon
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class UpdateReviewServiceTest {
    private val repository = mockk<ReviewRepository>()
    private val userRepository = mockk<UserRepository>()
    private val service = UpdateReviewService(repository, ReviewAuthorIconResolver(userRepository))

    private val reviewId = ReviewId.of("00000000-0000-0000-0000-000000000001")
    private val author = ReviewAuthorId.of("11111111-1111-1111-1111-111111111111")
    private val otherUser = ReviewAuthorId.of("22222222-2222-2222-2222-222222222222")

    private val existing =
        Review(
            id = reviewId,
            spotId = SpotId.of("kobe-port-tower"),
            rating = ReviewRating.of(3),
            comment = "普通でした",
            author = ReviewAuthor(name = "Alice", iconUrl = "https://example.com/before.webp", userId = author),
            createdAt = Instant.parse("2025-11-03T10:00:00Z"),
            language = Language.JA,
        )

    @Test
    fun `updateReview は rating と comment を差し替えて保存し返す`() {
        val saved = slot<Review>()
        every { repository.findById(reviewId) } returns existing
        every { repository.save(capture(saved)) } answers { saved.captured }
        givenProfile(icon = "https://example.com/before.webp")

        val result = service.updateReview(reviewId, ReviewRating.of(5), "最高でした", author)

        assertEquals(5, result.rating.value)
        assertEquals("最高でした", result.comment)
        assertEquals(existing.id, result.id)
        assertEquals(existing.author, result.author)
        verify(exactly = 1) { repository.save(saved.captured) }
    }

    // ---- 投稿者アイコン（#210）----

    private val profileId = User.Id.of(author.value)

    private fun givenProfile(icon: String?) {
        every { userRepository.findById(profileId) } returns User.create(profileId, "Alice", icon?.let(UserIcon::of))
    }

    private fun update(): Review {
        every { repository.findById(reviewId) } returns existing
        every { repository.save(any()) } answers { firstArg() }
        return service.updateReview(reviewId, ReviewRating.of(5), "最高でした", author)
    }

    @Test
    fun `updateReview は投稿後に変更したアイコンを投稿者アイコンにする`() {
        givenProfile(icon = "https://example.com/after.webp")

        assertEquals("https://example.com/after.webp", update().author.iconUrl)
    }

    @Test
    fun `updateReview はアイコンを未設定に戻した投稿者なら投稿時点のアイコンがあっても null`() {
        givenProfile(icon = null)

        assertNull(update().author.iconUrl)
    }

    @Test
    fun `updateReview はプロフィール行が無ければ投稿時点のアイコンを保つ`() {
        // 一覧もプロフィール行が無いときは投稿時点の値を返すため、編集直後と再取得後をそろえる。
        every { userRepository.findById(profileId) } returns null

        assertEquals("https://example.com/before.webp", update().author.iconUrl)
    }

    @Test
    fun `存在しない reviewId を渡すと ReviewNotFoundException をスローする`() {
        every { repository.findById(reviewId) } returns null

        assertFailsWith<ReviewNotFoundException> {
            service.updateReview(reviewId, ReviewRating.of(4), "更新", author)
        }
    }

    @Test
    fun `他人のレビューは編集できない`() {
        every { repository.findById(reviewId) } returns existing

        assertFailsWith<ReviewNotOwnedException> {
            service.updateReview(reviewId, ReviewRating.of(1), "改竄", otherUser)
        }

        verify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `投稿者不明の古いレビューは誰にも編集できない`() {
        // V17 以前の投稿は author_user_id が NULL。本人を確定できないので編集させない。
        every { repository.findById(reviewId) } returns
            existing.copy(author = ReviewAuthor(name = "Alice", userId = null))

        assertFailsWith<ReviewNotOwnedException> {
            service.updateReview(reviewId, ReviewRating.of(5), "更新", author)
        }

        verify(exactly = 0) { repository.save(any()) }
    }
}

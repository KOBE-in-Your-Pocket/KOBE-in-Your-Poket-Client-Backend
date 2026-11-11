package com.kobeinyourpocket.backend.application.tourism.command

import com.kobeinyourpocket.backend.application.user.command.UserNotFoundException
import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.domain.tourism.review.model.Review
import com.kobeinyourpocket.backend.domain.tourism.review.repository.ReviewRepository
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewAuthorId
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewRating
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId
import com.kobeinyourpocket.backend.domain.user.model.User
import com.kobeinyourpocket.backend.domain.user.repository.UserRepository
import com.kobeinyourpocket.backend.domain.user.vo.UserIcon
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class PostReviewServiceTest {
    private val repository = mockk<ReviewRepository>()
    private val userRepository = mockk<UserRepository>()
    private val service = PostReviewService(repository, ReviewAuthorResolver(userRepository))

    private val spotId = SpotId.of("kobe-port-tower")
    private val authorUserId = ReviewAuthorId.of("11111111-1111-1111-1111-111111111111")
    private val profileId = User.Id.of(authorUserId.value)

    init {
        // 既定はアイコン未設定のプロフィール。表示名・アイコンを見るテストだけ上書きする。
        every { userRepository.findById(profileId) } returns User.create(profileId, "Alice")
    }

    private fun post(): Review {
        val saved = slot<Review>()
        every { repository.save(capture(saved)) } answers { saved.captured }
        return service.postReview(spotId, ReviewRating.of(4), "素晴らしい景色", authorUserId, Language.JA)
    }

    @Test
    fun `postReview は Review を採番して保存し返す`() {
        val saved = slot<Review>()
        every { repository.save(capture(saved)) } answers { saved.captured }

        val result =
            service.postReview(
                spotId = spotId,
                rating = ReviewRating.of(4),
                comment = "素晴らしい景色",
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
        val result = post()

        // 本人削除・本人編集（#86）はこの id だけを根拠に判定する。
        assertEquals(authorUserId, result.author.userId)
    }

    @Test
    fun `postReview は ReviewId をサーバー側で採番する`() {
        val first = slot<Review>()
        val second = slot<Review>()
        every { repository.save(capture(first)) } answers { first.captured }
        every { repository.save(capture(second)) } answers { second.captured }

        val r1 = service.postReview(spotId, ReviewRating.of(5), "Good", authorUserId, Language.EN)
        val r2 = service.postReview(spotId, ReviewRating.of(3), "Okay", authorUserId, Language.EN)

        assert(r1.id != r2.id) { "採番した ID は毎回異なるはず" }
    }

    // ---- 投稿者の表示名（#93）----

    @Test
    fun `postReview は投稿者のプロフィールの表示名を投稿者名にする`() {
        every { userRepository.findById(profileId) } returns User.create(profileId, "神戸太郎")

        assertEquals("神戸太郎", post().author.name)
    }

    @Test
    fun `postReview はプロフィール行が無ければ UserNotFoundException で保存しない`() {
        // 行はサインイン時に必ず作られる。無いときにリクエストの名前へ戻すと、その経路だけ名前をかたれる。
        every { userRepository.findById(profileId) } returns null

        assertFailsWith<UserNotFoundException> {
            service.postReview(spotId, ReviewRating.of(4), "素晴らしい景色", authorUserId, Language.JA)
        }

        verify(exactly = 0) { repository.save(any()) }
    }

    // ---- 投稿者アイコン（#210）----

    @Test
    fun `postReview は投稿者のプロフィールのアイコンを投稿者アイコンにする`() {
        every { userRepository.findById(profileId) } returns
            User.create(profileId, "Alice", UserIcon.of("https://example.com/icon.webp"))

        assertEquals("https://example.com/icon.webp", post().author.iconUrl)
    }

    @Test
    fun `postReview はプロフィールのアイコンが未設定なら投稿者アイコンなし`() {
        assertNull(post().author.iconUrl)
    }
}

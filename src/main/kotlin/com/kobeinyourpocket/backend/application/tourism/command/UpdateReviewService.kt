package com.kobeinyourpocket.backend.application.tourism.command

import com.kobeinyourpocket.backend.application.tourism.ReviewNotFoundException
import com.kobeinyourpocket.backend.application.tourism.ReviewNotOwnedException
import com.kobeinyourpocket.backend.domain.tourism.review.model.Review
import com.kobeinyourpocket.backend.domain.tourism.review.repository.ReviewRepository
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewAuthorId
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewId
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewRating
import org.springframework.stereotype.Service

/**
 * レビュー更新ユースケース（write）。rating と comment のみ更新可能。
 *
 * 投稿者本人のみが更新できる（#86）。認証を通っただけの別ユーザーが reviewId を指定して
 * 他人の投稿を書き換えられないよう、ここで所有者を検証する。REST 層の `authenticated()` は
 * 「ログイン済みか」しか見ないため、本人判定はユースケース側の責務。
 *
 * 投稿者アイコンは投稿後に変わりうるため、編集時に現在のプロフィールから解決し直す（#210）。
 */
@Service
class UpdateReviewService(
    private val reviewRepository: ReviewRepository,
    private val authorIconResolver: ReviewAuthorIconResolver,
) {
    fun updateReview(
        reviewId: ReviewId,
        rating: ReviewRating,
        comment: String,
        requesterId: ReviewAuthorId,
    ): Review {
        val existing = reviewRepository.findById(reviewId) ?: throw ReviewNotFoundException(reviewId)
        if (!existing.author.isOwnedBy(requesterId)) throw ReviewNotOwnedException(reviewId)
        val iconUrl = authorIconResolver.resolve(requesterId, snapshot = existing.author.iconUrl)
        return reviewRepository.save(
            existing.copy(rating = rating, comment = comment, author = existing.author.copy(iconUrl = iconUrl)),
        )
    }
}

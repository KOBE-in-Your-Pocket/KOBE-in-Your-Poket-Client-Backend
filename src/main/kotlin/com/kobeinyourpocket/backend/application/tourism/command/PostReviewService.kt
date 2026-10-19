package com.kobeinyourpocket.backend.application.tourism.command

import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.domain.tourism.review.model.Review
import com.kobeinyourpocket.backend.domain.tourism.review.repository.ReviewRepository
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewAuthor
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewAuthorId
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewRating
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId
import org.springframework.stereotype.Service

/**
 * レビュー投稿ユースケース（write）。
 *
 * 表示名（[authorName]）はリクエストボディ由来だが、[authorUserId] は REST 層が JWT の
 * `sub` から渡す（クライアントの自己申告ではない）。この id が後の本人編集・本人削除
 * （#86）の判定根拠になるため、投稿時に必ず記録する。
 *
 * 投稿者アイコンはリクエストでは受け取らず、投稿者の現在のプロフィールから解決する（#210）。
 * 一覧は常にプロフィールから引くため、レビュー行に残す値はプロフィール行が無い場合の控えにすぎない。
 */
@Service
class PostReviewService(
    private val reviewRepository: ReviewRepository,
    private val authorIconResolver: ReviewAuthorIconResolver,
) {
    fun postReview(
        spotId: SpotId,
        rating: ReviewRating,
        comment: String,
        authorName: String,
        authorUserId: ReviewAuthorId,
        language: Language,
    ): Review {
        val review =
            Review.create(
                spotId = spotId,
                rating = rating,
                comment = comment,
                author =
                    ReviewAuthor(
                        name = authorName,
                        iconUrl = authorIconResolver.resolve(authorUserId, snapshot = null),
                        userId = authorUserId,
                    ),
                language = language,
            )
        return reviewRepository.save(review)
    }
}

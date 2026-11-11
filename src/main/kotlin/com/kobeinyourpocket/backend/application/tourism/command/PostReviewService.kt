package com.kobeinyourpocket.backend.application.tourism.command

import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.domain.tourism.review.model.Review
import com.kobeinyourpocket.backend.domain.tourism.review.repository.ReviewRepository
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewAuthorId
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewRating
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId
import org.springframework.stereotype.Service

/**
 * レビュー投稿ユースケース（write）。
 *
 * [authorUserId] は REST 層が JWT の `sub` から渡す（クライアントの自己申告ではない）。
 * この id が後の本人編集・本人削除（#86）の判定根拠になるため、投稿時に必ず記録する。
 *
 * 投稿者の表示名・アイコンはリクエストでは受け取らず、この id に紐づくプロフィールから
 * 解決する（#93 / #210）。プロフィール行が無ければ投稿できない（[ReviewAuthorResolver.forNewReview]）。
 */
@Service
class PostReviewService(
    private val reviewRepository: ReviewRepository,
    private val authorResolver: ReviewAuthorResolver,
) {
    fun postReview(
        spotId: SpotId,
        rating: ReviewRating,
        comment: String,
        authorUserId: ReviewAuthorId,
        language: Language,
    ): Review {
        val review =
            Review.create(
                spotId = spotId,
                rating = rating,
                comment = comment,
                author = authorResolver.forNewReview(authorUserId),
                language = language,
            )
        return reviewRepository.save(review)
    }
}

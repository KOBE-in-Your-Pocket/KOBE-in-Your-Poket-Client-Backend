package com.kobeinyourpocket.backend.domain.tourism.review.model

import com.kobeinyourpocket.backend.domain.tourism.localization.Language
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewAuthor
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewId
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewRating
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId
import java.time.Instant

/**
 * [エンティティ] レビュー。
 *
 * 観光スポット（[SpotId]）に対してユーザーが投稿する星評価とコメントを表す。
 * 投稿者は [ReviewAuthor] VO で表現する（PublicUser 確定後に差し替え可能な薄い seam）。
 *
 * Client `Review` の id / spotId / rating / comment / author / postedAt / language に対応。
 */
data class Review(
    val id: ReviewId,
    val spotId: SpotId,
    val rating: ReviewRating,
    val comment: String,
    val author: ReviewAuthor,
    val createdAt: Instant,
    val language: Language,
) {
    init {
        require(comment.isNotBlank()) { "comment must not be blank" }
        require(comment.length <= MAX_COMMENT_LENGTH) {
            "comment must be at most $MAX_COMMENT_LENGTH characters, got ${comment.length}"
        }
    }

    companion object {
        const val MAX_COMMENT_LENGTH = 1_000

        /**
         * レビューを新規投稿する。[ReviewId] はサーバー側で自動採番する。
         *
         * [createdAt] は DI や固定値での上書きが必要なテスト以外では省略してよい。
         */
        fun create(
            spotId: SpotId,
            rating: ReviewRating,
            comment: String,
            author: ReviewAuthor,
            language: Language,
            createdAt: Instant = Instant.now(),
        ): Review =
            Review(
                id = ReviewId.generate(),
                spotId = spotId,
                rating = rating,
                comment = comment,
                author = author,
                createdAt = createdAt,
                language = language,
            )
    }
}

package com.kobeinyourpocket.backend.domain.tourism.aggregate

import com.kobeinyourpocket.backend.domain.tourism.vo.Language
import com.kobeinyourpocket.backend.domain.tourism.vo.ReviewId
import com.kobeinyourpocket.backend.domain.tourism.vo.ReviewRating
import com.kobeinyourpocket.backend.domain.tourism.vo.SpotId
import java.time.Instant

/**
 * レビュー集約ルート。
 *
 * 観光スポット（[SpotId]）に対してユーザーが投稿する星評価とコメントを表す。
 * 投稿者は認証未確定のため [authorName] 文字列で表現する薄い seam（User PBI で本認証に置き換え）。
 *
 * Client `Review` の id / spotId / rating / comment / authorName / postedAt / language に対応。
 */
data class Review(
    val id: ReviewId,
    val spotId: SpotId,
    val rating: ReviewRating,
    val comment: String,
    val authorName: String,
    val createdAt: Instant,
    val language: Language,
) {
    init {
        require(comment.isNotBlank()) { "comment must not be blank" }
        require(authorName.isNotBlank()) { "authorName must not be blank" }
        require(comment.length <= MAX_COMMENT_LENGTH) {
            "comment must be at most $MAX_COMMENT_LENGTH characters, got ${comment.length}"
        }
        require(authorName.length <= MAX_AUTHOR_NAME_LENGTH) {
            "authorName must be at most $MAX_AUTHOR_NAME_LENGTH characters, got ${authorName.length}"
        }
    }

    companion object {
        const val MAX_COMMENT_LENGTH = 1_000
        const val MAX_AUTHOR_NAME_LENGTH = 100

        /**
         * レビューを新規投稿する。[ReviewId] はサーバー側で自動採番する。
         *
         * [createdAt] は DI や固定値での上書きが必要なテスト以外では省略してよい。
         */
        fun create(
            spotId: SpotId,
            rating: ReviewRating,
            comment: String,
            authorName: String,
            language: Language,
            createdAt: Instant = Instant.now(),
        ): Review =
            Review(
                id = ReviewId.generate(),
                spotId = spotId,
                rating = rating,
                comment = comment,
                authorName = authorName,
                createdAt = createdAt,
                language = language,
            )
    }
}

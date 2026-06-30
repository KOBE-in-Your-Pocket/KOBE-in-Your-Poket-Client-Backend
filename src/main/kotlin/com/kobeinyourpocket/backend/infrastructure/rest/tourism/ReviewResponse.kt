package com.kobeinyourpocket.backend.infrastructure.rest.tourism

import com.fasterxml.jackson.annotation.JsonInclude
import com.kobeinyourpocket.backend.application.tourism.query.ReviewView
import com.kobeinyourpocket.backend.domain.tourism.aggregate.Review
import java.time.Instant

/**
 * レビュー系エンドポイント共通レスポンス（client `domain/review.ts` の `Review` 形 / §8）。
 */
data class ReviewResponse(
    val id: String,
    val rating: RatingResponse,
    val comment: String,
    val author: AuthorResponse,
    val postedAt: Instant,
    val language: String,
) {
    data class RatingResponse(val value: Int)

    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class AuthorResponse(
        val name: String,
        val iconUrl: String?,
    )

    companion object {
        fun from(review: Review): ReviewResponse =
            ReviewResponse(
                id = review.id.toString(),
                rating = RatingResponse(review.rating.value),
                comment = review.comment,
                author = AuthorResponse(review.author.name, review.author.iconUrl),
                postedAt = review.createdAt,
                language = review.language.code,
            )

        fun from(view: ReviewView): ReviewResponse =
            ReviewResponse(
                id = view.id,
                rating = RatingResponse(view.rating),
                comment = view.comment,
                author = AuthorResponse(view.authorName, view.authorIconUrl),
                postedAt = view.createdAt,
                language = view.language,
            )
    }
}

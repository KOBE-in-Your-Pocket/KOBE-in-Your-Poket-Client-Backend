package com.kobeinyourpocket.backend.infrastructure.rest.tourism

import com.fasterxml.jackson.annotation.JsonInclude
import com.kobeinyourpocket.backend.application.tourism.query.ReviewView
import com.kobeinyourpocket.backend.domain.tourism.review.model.Review
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
    /**
     * 運営が通報を承認し、非表示に同意した（不適切と判断した）口コミか。true なら Client はアプリ全体で非表示にする。
     *
     * 判定は読み取り（GET）でのみ行う。POST / PUT の応答は書き込んだ集約から作るため常に false。
     */
    val hiddenByReport: Boolean,
) {
    data class RatingResponse(
        val value: Int,
    )

    /**
     * 投稿者。[id] は Client が「自分の投稿か」を判定するために必要（#86）。
     *
     * V17 以前の投稿は投稿者を特定できず null になり、`NON_NULL` によりキー自体が落ちる。
     * Client 側はキー欠落を「自分の投稿ではない」として扱う。
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class AuthorResponse(
        val id: String?,
        val name: String,
        val iconUrl: String?,
    )

    companion object {
        fun from(review: Review): ReviewResponse =
            ReviewResponse(
                id = review.id.toString(),
                rating = RatingResponse(review.rating.value),
                comment = review.comment,
                author =
                    AuthorResponse(
                        id = review.author.userId?.toString(),
                        name = review.author.name,
                        iconUrl = review.author.iconUrl,
                    ),
                postedAt = review.createdAt,
                language = review.language.code,
                hiddenByReport = false,
            )

        fun from(view: ReviewView): ReviewResponse =
            ReviewResponse(
                id = view.id,
                rating = RatingResponse(view.rating),
                comment = view.comment,
                author =
                    AuthorResponse(
                        id = view.authorUserId,
                        name = view.authorName,
                        iconUrl = view.authorIconUrl,
                    ),
                postedAt = view.createdAt,
                language = view.language,
                hiddenByReport = view.hiddenByReport,
            )
    }
}

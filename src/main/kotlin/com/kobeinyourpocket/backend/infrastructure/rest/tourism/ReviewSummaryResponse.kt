package com.kobeinyourpocket.backend.infrastructure.rest.tourism

import com.fasterxml.jackson.annotation.JsonInclude
import com.kobeinyourpocket.backend.application.tourism.query.ReviewPageView
import com.kobeinyourpocket.backend.application.tourism.query.ReviewSummaryView
import java.time.Instant

/**
 * `GET /api/v1/tourism/reviews` のレスポンス封筒（#165）。
 *
 * `data` + `meta` の形はユーザー一覧（#151）に合わせる。`meta` にページ情報を載せるのは、
 * 管理画面のページャが総件数を必要とするため。
 */
data class ReviewListResponse(
    val data: List<ReviewSummaryResponse>,
    val meta: ReviewListMetaResponse,
) {
    companion object {
        fun from(view: ReviewPageView): ReviewListResponse =
            ReviewListResponse(
                data = view.reviews.map(ReviewSummaryResponse::from),
                meta =
                    ReviewListMetaResponse(
                        page = view.page,
                        size = view.size,
                        totalElements = view.totalElements,
                        totalPages = view.totalPages,
                    ),
            )
    }
}

/**
 * 一覧 1 件分。スポット別の [ReviewResponse] に `spotId` / `spotName` を足した形。
 *
 * `spotName` は要求言語で解決済み。`comment` / `author.name` は**投稿時の言語のまま**で、
 * `language` がその言語を表す（運営はスポット名を自分の言語で読み、本文は原文で確認する）。
 */
data class ReviewSummaryResponse(
    val id: String,
    val spotId: String,
    val spotName: String,
    val rating: ReviewResponse.RatingResponse,
    val comment: String,
    val author: ReviewResponse.AuthorResponse,
    val postedAt: Instant,
    val language: String,
) {
    companion object {
        fun from(view: ReviewSummaryView): ReviewSummaryResponse =
            ReviewSummaryResponse(
                id = view.id,
                spotId = view.spotId,
                spotName = view.spotName,
                rating = ReviewResponse.RatingResponse(view.rating),
                comment = view.comment,
                author = ReviewResponse.AuthorResponse(view.authorName, view.authorIconUrl),
                postedAt = view.createdAt,
                language = view.language,
            )
    }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ReviewListMetaResponse(
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

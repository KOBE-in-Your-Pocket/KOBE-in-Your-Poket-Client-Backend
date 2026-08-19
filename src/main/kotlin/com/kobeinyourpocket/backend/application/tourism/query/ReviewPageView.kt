package com.kobeinyourpocket.backend.application.tourism.query

/**
 * 運営向けレビュー一覧の 1 ページ分（read / #165）。
 *
 * 総件数を持つのは、管理画面のページャが「全 N 件中」を描くのに必要なため。
 */
data class ReviewPageView(
    val reviews: List<ReviewSummaryView>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
) {
    /** 総ページ数。[size] が 0 以下になる経路は [ListAllReviewsService] が塞ぐ。 */
    val totalPages: Int
        get() = if (size <= 0) 0 else ((totalElements + size - 1) / size).toInt()
}

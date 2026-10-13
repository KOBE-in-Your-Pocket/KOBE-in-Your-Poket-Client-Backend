package com.kobeinyourpocket.backend.infrastructure.rest.report

import com.kobeinyourpocket.backend.application.report.query.ReportedReviewView
import com.kobeinyourpocket.backend.application.report.query.ReviewReportGroupView
import com.kobeinyourpocket.backend.application.report.query.ReviewReportItemView
import com.kobeinyourpocket.backend.application.report.query.ReviewReportPageView
import java.time.Instant

/**
 * `GET /api/v1/reports/reviews` のレスポンス（#145）。運営向け一覧と同じ `data` + `meta` の形。
 */
data class ReviewReportListResponse(
    val data: List<ReviewReportGroupResponse>,
    val meta: Meta,
) {
    data class Meta(
        val page: Int,
        val size: Int,
        val totalElements: Long,
        val totalPages: Int,
    )

    companion object {
        fun from(view: ReviewReportPageView): ReviewReportListResponse =
            ReviewReportListResponse(
                data = view.groups.map(ReviewReportGroupResponse::from),
                meta =
                    Meta(
                        page = view.page,
                        size = view.size,
                        totalElements = view.totalElements,
                        totalPages = view.totalPages,
                    ),
            )
    }
}

/** 通報された口コミ 1 件分。[review] は口コミが削除済みなら null。 */
data class ReviewReportGroupResponse(
    val reviewId: String,
    val review: ReviewBody?,
    val reportCount: Int,
    val openCount: Int,
    val reasonCounts: Map<String, Int>,
    val latestReportedAt: Instant,
    val reports: List<ReportItem>,
) {
    /** `spotName` は要求言語で解決済み。`comment` / `author.name` は投稿時の言語のまま（`language`）。 */
    data class ReviewBody(
        val spotId: String,
        val spotName: String,
        val rating: Int,
        val comment: String,
        val author: Person,
        val language: String,
        val postedAt: Instant,
    )

    data class ReportItem(
        val id: String,
        val reason: String,
        val description: String?,
        val status: String,
        val reporter: Person,
        val createdAt: Instant,
        val handledBy: String?,
        val handledAt: Instant?,
    )

    /** `id` は投稿者不明の古い口コミで null。`name` は通報者のプロフィール行が無いとき null。 */
    data class Person(
        val id: String?,
        val name: String?,
    )

    companion object {
        fun from(view: ReviewReportGroupView): ReviewReportGroupResponse =
            ReviewReportGroupResponse(
                reviewId = view.reviewId,
                review = view.review?.let(::toReviewBody),
                reportCount = view.reportCount,
                openCount = view.openCount,
                reasonCounts = view.reasonCounts,
                latestReportedAt = view.latestReportedAt,
                reports = view.reports.map(::toReportItem),
            )

        private fun toReviewBody(view: ReportedReviewView): ReviewBody =
            ReviewBody(
                spotId = view.spotId,
                spotName = view.spotName,
                rating = view.rating,
                comment = view.comment,
                author = Person(id = view.authorUserId, name = view.authorName),
                language = view.language,
                postedAt = view.postedAt,
            )

        private fun toReportItem(view: ReviewReportItemView): ReportItem =
            ReportItem(
                id = view.id,
                reason = view.reason,
                description = view.description,
                status = view.status,
                reporter = Person(id = view.reporterUserId, name = view.reporterName),
                createdAt = view.createdAt,
                handledBy = view.handledBy,
                handledAt = view.handledAt,
            )
    }
}

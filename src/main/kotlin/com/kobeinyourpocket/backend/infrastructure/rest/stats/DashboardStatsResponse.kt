package com.kobeinyourpocket.backend.infrastructure.rest.stats

import com.kobeinyourpocket.backend.application.stats.query.DashboardStatsView
import com.kobeinyourpocket.backend.application.stats.query.EntityCountsView
import com.kobeinyourpocket.backend.application.stats.query.PopularSpotView
import com.kobeinyourpocket.backend.application.stats.query.RecentReviewView
import java.time.Instant

/**
 * `GET /api/v1/stats` のレスポンス（管理画面ダッシュボード用 / #169）。
 *
 * 「今月・先月」を項目ごとに分けて返すのは、前月比の計算式（増減率にするか差分にするか）を
 * 画面側に委ねるため。サーバーが率を計算して返すと、先月 0 件のときの見せ方まで
 * API 側で決めることになる。
 */
data class DashboardStatsResponse(
    val totals: CountsResponse,
    val newUsers: PeriodCountResponse,
    val newSpots: PeriodCountResponse,
    val newReviews: PeriodCountResponse,
    val popularSpots: List<PopularSpotResponse>,
    val recentReviews: List<RecentReviewResponse>,
) {
    data class CountsResponse(
        val users: Long,
        val spots: Long,
        val reviews: Long,
    )

    /** 今月・先月の登録件数。境界は Asia/Tokyo の月初。 */
    data class PeriodCountResponse(
        val thisMonth: Long,
        val lastMonth: Long,
    )

    data class PopularSpotResponse(
        val spotId: String,
        val name: String,
        val reviewCount: Long,
    )

    /** rating は既存の [com.kobeinyourpocket.backend.infrastructure.rest.tourism.ReviewResponse] と同じ入れ子形にそろえる。 */
    data class RecentReviewResponse(
        val id: String,
        val spotId: String,
        val spotName: String,
        val authorName: String,
        val rating: RatingResponse,
        val postedAt: Instant,
        val language: String,
    ) {
        data class RatingResponse(
            val value: Int,
        )
    }

    companion object {
        fun from(view: DashboardStatsView): DashboardStatsResponse =
            DashboardStatsResponse(
                totals = view.totals.toCountsResponse(),
                newUsers = PeriodCountResponse(view.thisMonth.users, view.lastMonth.users),
                newSpots = PeriodCountResponse(view.thisMonth.spots, view.lastMonth.spots),
                newReviews = PeriodCountResponse(view.thisMonth.reviews, view.lastMonth.reviews),
                popularSpots = view.popularSpots.map(::toPopularSpotResponse),
                recentReviews = view.recentReviews.map(::toRecentReviewResponse),
            )

        private fun EntityCountsView.toCountsResponse(): CountsResponse = CountsResponse(users, spots, reviews)

        private fun toPopularSpotResponse(view: PopularSpotView): PopularSpotResponse =
            PopularSpotResponse(
                spotId = view.spotId,
                name = view.name,
                reviewCount = view.reviewCount,
            )

        private fun toRecentReviewResponse(view: RecentReviewView): RecentReviewResponse =
            RecentReviewResponse(
                id = view.id,
                spotId = view.spotId,
                spotName = view.spotName,
                authorName = view.authorName,
                rating = RecentReviewResponse.RatingResponse(view.rating),
                postedAt = view.postedAt,
                language = view.language,
            )
    }
}

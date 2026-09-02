package com.kobeinyourpocket.backend.application.stats.query

import com.kobeinyourpocket.backend.domain.common.localization.Language
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

/**
 * 管理画面ダッシュボードの集計ユースケース（read / #169）。domain 集約を経由せず [DashboardStatsQuery] port へ委譲する。
 *
 * 「今月」の境界判定をここに置く。運営は日本国内で使うため [ZONE]（Asia/Tokyo）の月初を境界にする。
 * UTC で切ると月初・月末の 9 時間ぶんが前月にずれ、運営の感覚と数字が合わなくなる。
 *
 * 上位 N 件は API を単純に保つため固定（[POPULAR_SPOT_LIMIT] / [RECENT_REVIEW_LIMIT]）。
 * 可変にする必要が出た時点で `?limit=` を足す。
 */
@Service
class GetDashboardStatsService(
    private val dashboardStatsQuery: DashboardStatsQuery,
    /** テストから固定時刻を渡すための時計。Bean が無い場合は [ZONE] のシステム時計を使う。 */
    private val clock: Clock = Clock.system(ZONE),
) {
    /** [language] はスポット名の解決にのみ効く（投稿者名は投稿時の言語のまま）。 */
    fun getStats(language: Language): DashboardStatsView {
        val thisMonthStart = LocalDate.now(clock.withZone(ZONE)).withDayOfMonth(1)
        val nextMonthStart = thisMonthStart.plusMonths(1)
        val lastMonthStart = thisMonthStart.minusMonths(1)

        return DashboardStatsView(
            totals = dashboardStatsQuery.countAll(),
            thisMonth =
                dashboardStatsQuery.countCreatedIn(
                    from = thisMonthStart.atStartOfDay(ZONE).toInstant(),
                    until = nextMonthStart.atStartOfDay(ZONE).toInstant(),
                ),
            lastMonth =
                dashboardStatsQuery.countCreatedIn(
                    from = lastMonthStart.atStartOfDay(ZONE).toInstant(),
                    until = thisMonthStart.atStartOfDay(ZONE).toInstant(),
                ),
            popularSpots = dashboardStatsQuery.findPopularSpots(language, POPULAR_SPOT_LIMIT),
            recentReviews = dashboardStatsQuery.findRecentReviews(language, RECENT_REVIEW_LIMIT),
        )
    }

    companion object {
        /** 「今月」の境界を判定するタイムゾーン。運営は日本国内で使う前提。 */
        val ZONE: ZoneId = ZoneId.of("Asia/Tokyo")

        /** 人気スポットの表示件数（ADMIN のダッシュボードは Top5 固定）。 */
        const val POPULAR_SPOT_LIMIT = 5

        /** 直近レビューの表示件数。 */
        const val RECENT_REVIEW_LIMIT = 5
    }
}

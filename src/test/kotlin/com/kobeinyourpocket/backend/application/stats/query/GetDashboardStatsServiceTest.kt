package com.kobeinyourpocket.backend.application.stats.query

import com.kobeinyourpocket.backend.domain.common.localization.Language
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 月境界の判定と上位 N 件の固定（#169）。
 *
 * 「今月」は Asia/Tokyo の月初で切る。UTC で切ると月初 9 時間ぶんが前月に入り、
 * 運営が見る「今月の新規ユーザー」が実感とずれる。
 */
class GetDashboardStatsServiceTest {
    /** 呼ばれた引数を記録するだけの [DashboardStatsQuery]。 */
    private class RecordingQuery : DashboardStatsQuery {
        val requestedRanges = mutableListOf<Pair<Instant, Instant>>()
        var requestedLanguage: Language? = null
        var requestedPopularLimit: Int? = null
        var requestedRecentLimit: Int? = null

        override fun countAll(): EntityCountsView = EntityCountsView(users = 12, spots = 15, reviews = 5)

        override fun countCreatedIn(
            from: Instant,
            until: Instant,
        ): EntityCountsView {
            requestedRanges += from to until
            return EntityCountsView(users = 1, spots = 2, reviews = 3)
        }

        override fun findPopularSpots(
            language: Language,
            limit: Int,
        ): List<PopularSpotView> {
            requestedLanguage = language
            requestedPopularLimit = limit
            return emptyList()
        }

        override fun findRecentReviews(
            language: Language,
            limit: Int,
        ): List<RecentReviewView> {
            requestedRecentLimit = limit
            return emptyList()
        }
    }

    private fun serviceAt(now: String): Pair<GetDashboardStatsService, RecordingQuery> {
        val query = RecordingQuery()
        val clock = Clock.fixed(Instant.parse(now), ZoneOffset.UTC)
        return GetDashboardStatsService(query, clock) to query
    }

    @Test
    fun `今月は Asia_Tokyo の月初から翌月初までの半開区間で数える`() {
        val (service, query) = serviceAt("2026-09-10T05:00:00Z")

        service.getStats(Language.JA)

        val (thisMonthFrom, thisMonthUntil) = query.requestedRanges[0]
        // 2026-09-01T00:00+09:00 / 2026-10-01T00:00+09:00
        assertEquals(Instant.parse("2026-08-31T15:00:00Z"), thisMonthFrom)
        assertEquals(Instant.parse("2026-09-30T15:00:00Z"), thisMonthUntil)
    }

    @Test
    fun `先月は前月初から今月初までを数える`() {
        val (service, query) = serviceAt("2026-09-10T05:00:00Z")

        service.getStats(Language.JA)

        val (lastMonthFrom, lastMonthUntil) = query.requestedRanges[1]
        assertEquals(Instant.parse("2026-07-31T15:00:00Z"), lastMonthFrom)
        assertEquals(Instant.parse("2026-08-31T15:00:00Z"), lastMonthUntil)
    }

    @Test
    fun `UTC ではまだ前月でも日本時間で月が変わっていれば新しい月として扱う`() {
        // UTC では 8/31 15:00 だが、日本時間では 9/1 00:00。
        val (service, query) = serviceAt("2026-08-31T15:00:00Z")

        service.getStats(Language.JA)

        assertEquals(Instant.parse("2026-08-31T15:00:00Z"), query.requestedRanges[0].first)
    }

    @Test
    fun `日本時間で月末のうちは前の月として扱う`() {
        // 日本時間 2026-08-31T23:59:59。
        val (service, query) = serviceAt("2026-08-31T14:59:59Z")

        service.getStats(Language.JA)

        assertEquals(Instant.parse("2026-07-31T15:00:00Z"), query.requestedRanges[0].first)
    }

    @Test
    fun `上位一覧は 5 件で固定し、要求言語をそのまま渡す`() {
        val (service, query) = serviceAt("2026-09-10T05:00:00Z")

        service.getStats(Language.EN)

        assertEquals(GetDashboardStatsService.POPULAR_SPOT_LIMIT, query.requestedPopularLimit)
        assertEquals(GetDashboardStatsService.RECENT_REVIEW_LIMIT, query.requestedRecentLimit)
        assertEquals(5, query.requestedPopularLimit)
        assertEquals(Language.EN, query.requestedLanguage)
    }

    @Test
    fun `総数と期間集計をそのまま View に載せる`() {
        val (service, _) = serviceAt("2026-09-10T05:00:00Z")

        val stats = service.getStats(Language.JA)

        assertEquals(EntityCountsView(users = 12, spots = 15, reviews = 5), stats.totals)
        assertEquals(EntityCountsView(users = 1, spots = 2, reviews = 3), stats.thisMonth)
        assertEquals(EntityCountsView(users = 1, spots = 2, reviews = 3), stats.lastMonth)
    }
}

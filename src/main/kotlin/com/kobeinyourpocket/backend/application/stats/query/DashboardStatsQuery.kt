package com.kobeinyourpocket.backend.application.stats.query

import com.kobeinyourpocket.backend.domain.common.localization.Language
import java.time.Instant

/**
 * ダッシュボード集計の read port（CQRS-lite / #169）。
 *
 * 集計は複数コンテキスト（user / tourism）のテーブルをまたぐ純粋な読み取りのため、
 * domain 集約を経由せず query adapter から直接読む。
 */
interface DashboardStatsQuery {
    /** 全期間の総数を 1 リクエストで返す。 */
    fun countAll(): EntityCountsView

    /**
     * [from] 以上 [until] 未満に登録された件数を返す。
     *
     * 月境界の判定は application 層（[GetDashboardStatsService]）の責務で、
     * ここは受け取った区間をそのまま使う。半開区間なのは、月末 23:59:59.999… を
     * 書く必要を無くし、境界の 1 件が二重計上されないようにするため。
     */
    fun countCreatedIn(
        from: Instant,
        until: Instant,
    ): EntityCountsView

    /** レビュー数の多い順にスポットを [limit] 件返す。スポット名は [language] で解決する。 */
    fun findPopularSpots(
        language: Language,
        limit: Int,
    ): List<PopularSpotView>

    /** 投稿の新しい順にレビューを [limit] 件返す。スポット名は [language] で解決する。 */
    fun findRecentReviews(
        language: Language,
        limit: Int,
    ): List<RecentReviewView>
}

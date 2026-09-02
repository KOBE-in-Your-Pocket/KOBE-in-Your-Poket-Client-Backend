package com.kobeinyourpocket.backend.infrastructure.query.stats

import com.kobeinyourpocket.backend.application.stats.query.DashboardStatsQuery
import com.kobeinyourpocket.backend.application.stats.query.EntityCountsView
import com.kobeinyourpocket.backend.application.stats.query.PopularSpotView
import com.kobeinyourpocket.backend.application.stats.query.RecentReviewView
import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.infrastructure.query.common.JdbcTimestamps
import jakarta.persistence.EntityManager
import jakarta.persistence.Query
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.ZoneOffset

/**
 * [DashboardStatsQuery] の JPA 実装（#169）。
 *
 * 集計は SQL 側（COUNT / GROUP BY）で行い、行そのものはアプリへ持ち込まない。
 * 一覧 API を取得してフロントで数える方式（ADMIN の暫定実装）が 200 件上限で
 * 部分集計になっていたのを解消するのが本 Query の目的。
 */
@Repository
class DashboardStatsQueryJpa(
    private val entityManager: EntityManager,
) : DashboardStatsQuery {
    override fun countAll(): EntityCountsView =
        entityManager
            .createNativeQuery(COUNT_ALL)
            .resultRow()
            .let(::toCountsView)

    /**
     * 期間は半開区間 `[from, until)`。境界の 1 件が今月と先月へ二重計上されない。
     *
     * TIMESTAMPTZ 列との比較に [Instant] をそのまま渡すとドライバによって型解決が割れるため、
     * UTC の [java.time.OffsetDateTime] へ寄せて渡す（読み取り側の [JdbcTimestamps] と対の関係）。
     */
    override fun countCreatedIn(
        from: Instant,
        until: Instant,
    ): EntityCountsView =
        entityManager
            .createNativeQuery(COUNT_CREATED_IN)
            .setParameter("from", from.atOffset(ZoneOffset.UTC))
            .setParameter("until", until.atOffset(ZoneOffset.UTC))
            .resultRow()
            .let(::toCountsView)

    override fun findPopularSpots(
        language: Language,
        limit: Int,
    ): List<PopularSpotView> {
        @Suppress("UNCHECKED_CAST")
        val rows =
            entityManager
                .createNativeQuery(SELECT_POPULAR_SPOTS)
                .apply {
                    setParameter("language", language.code)
                    setParameter("fallback", Language.DEFAULT.code)
                    setParameter("limit", limit)
                }.resultList as List<Array<Any?>>

        return rows.map { row ->
            PopularSpotView(
                spotId = row[PopularSpotColumn.SPOT_ID] as String,
                name = row[PopularSpotColumn.NAME] as String,
                reviewCount = (row[PopularSpotColumn.REVIEW_COUNT] as Number).toLong(),
            )
        }
    }

    override fun findRecentReviews(
        language: Language,
        limit: Int,
    ): List<RecentReviewView> {
        @Suppress("UNCHECKED_CAST")
        val rows =
            entityManager
                .createNativeQuery(SELECT_RECENT_REVIEWS)
                .apply {
                    setParameter("language", language.code)
                    setParameter("fallback", Language.DEFAULT.code)
                    setParameter("limit", limit)
                }.resultList as List<Array<Any?>>

        return rows.map { row ->
            RecentReviewView(
                id = row[RecentReviewColumn.ID] as String,
                spotId = row[RecentReviewColumn.SPOT_ID] as String,
                spotName = row[RecentReviewColumn.SPOT_NAME] as String,
                authorName = row[RecentReviewColumn.AUTHOR_NAME] as String,
                rating = (row[RecentReviewColumn.RATING] as Number).toInt(),
                postedAt = JdbcTimestamps.toInstant(row[RecentReviewColumn.CREATED_AT]),
                language = row[RecentReviewColumn.LANGUAGE] as String,
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun Query.resultRow(): Array<Any?> = (resultList as List<Array<Any?>>).single()

    private fun toCountsView(row: Array<Any?>): EntityCountsView =
        EntityCountsView(
            users = (row[CountColumn.USERS] as Number).toLong(),
            spots = (row[CountColumn.SPOTS] as Number).toLong(),
            reviews = (row[CountColumn.REVIEWS] as Number).toLong(),
        )

    /** [COUNT_ALL] / [COUNT_CREATED_IN] の列順。両 SQL で共通のため 1 つにまとめている。 */
    private object CountColumn {
        const val USERS = 0
        const val SPOTS = 1
        const val REVIEWS = 2
    }

    /** [SELECT_POPULAR_SPOTS] の列順と対応する index。列の並び替え時は両方を合わせて更新すること。 */
    private object PopularSpotColumn {
        const val SPOT_ID = 0
        const val NAME = 1
        const val REVIEW_COUNT = 2
    }

    /** [SELECT_RECENT_REVIEWS] の列順と対応する index。列の並び替え時は両方を合わせて更新すること。 */
    private object RecentReviewColumn {
        const val ID = 0
        const val SPOT_ID = 1
        const val SPOT_NAME = 2
        const val AUTHOR_NAME = 3
        const val RATING = 4
        const val CREATED_AT = 5
        const val LANGUAGE = 6
    }

    private companion object {
        /**
         * 3 つの COUNT を 1 往復で取る。件数だけを返すので、テーブルが伸びても
         * 転送量は変わらない（一覧 API を全件取得していた暫定実装との差はここ）。
         */
        val COUNT_ALL =
            """
            SELECT
                (SELECT count(*) FROM users)  AS user_count,
                (SELECT count(*) FROM spot)   AS spot_count,
                (SELECT count(*) FROM review) AS review_count
            """.trimIndent()

        val COUNT_CREATED_IN =
            """
            SELECT
                (SELECT count(*) FROM users  WHERE created_at >= :from AND created_at < :until) AS user_count,
                (SELECT count(*) FROM spot   WHERE created_at >= :from AND created_at < :until) AS spot_count,
                (SELECT count(*) FROM review WHERE created_at >= :from AND created_at < :until) AS review_count
            """.trimIndent()

        /**
         * スポット名は要求言語 → en → spot_id の順で解決する（横断レビュー一覧と同じ形）。
         * `COALESCE` の最後に `r.spot_id` を置くのは、en 欠けの spot があっても null を返さないため。
         * ここが null になると `as String` で ClassCastException になり、ダッシュボード全体が 500 になる。
         *
         * 件数が同じスポットが並ぶと順位が不定になるため、`spot_id` を第 2 キーに置いて
         * 全順序にする（同数でも毎回同じ並びで返る）。
         *
         * レビューが 1 件も無いスポットは行が立たない。ダッシュボードは「人気」を出す枠で、
         * 0 件のスポットを並べても運営の判断材料にならないため `review` 側を起点にしている。
         */
        val SELECT_POPULAR_SPOTS =
            """
            SELECT
                r.spot_id,
                COALESCE(l_req.name, l_fallback.name, r.spot_id) AS spot_name,
                count(*) AS review_count
            FROM review r
            LEFT JOIN spot_localization l_req
                ON r.spot_id = l_req.spot_id AND l_req.language = :language
            LEFT JOIN spot_localization l_fallback
                ON r.spot_id = l_fallback.spot_id AND l_fallback.language = :fallback
            GROUP BY r.spot_id, l_req.name, l_fallback.name
            ORDER BY count(*) DESC, r.spot_id
            LIMIT :limit
            """.trimIndent()

        /**
         * `id` を VARCHAR にキャストして取り出すのは、uuid 列の Java 型が実行環境で割れるため。
         * PostgreSQL（pgjdbc）は [java.util.UUID]、H2 は byte 配列を返し、後者を `toString()` すると
         * `[B@6cdd01af` のような配列の参照表現が API に出る（[com.kobeinyourpocket.backend.infrastructure.query.common.JdbcTimestamps]
         * と同じ種類の環境差）。SQL 側で文字列に寄せれば、どちらでも同じ値になる。
         *
         * created_at だけでは同時刻の行の順序が不定になるため、一意な id を第 2 キーに置く。
         * コメント本文は返さない（[com.kobeinyourpocket.backend.application.stats.query.RecentReviewView] 参照）。
         */
        val SELECT_RECENT_REVIEWS =
            """
            SELECT
                CAST(r.id AS VARCHAR) AS review_id,
                r.spot_id,
                COALESCE(l_req.name, l_fallback.name, r.spot_id) AS spot_name,
                r.author_name,
                r.rating,
                r.created_at,
                r.language
            FROM review r
            LEFT JOIN spot_localization l_req
                ON r.spot_id = l_req.spot_id AND l_req.language = :language
            LEFT JOIN spot_localization l_fallback
                ON r.spot_id = l_fallback.spot_id AND l_fallback.language = :fallback
            ORDER BY r.created_at DESC, r.id
            LIMIT :limit
            """.trimIndent()
    }
}

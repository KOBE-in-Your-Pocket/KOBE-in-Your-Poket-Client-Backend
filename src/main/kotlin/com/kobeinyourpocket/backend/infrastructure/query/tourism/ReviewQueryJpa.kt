package com.kobeinyourpocket.backend.infrastructure.query.tourism

import com.kobeinyourpocket.backend.application.tourism.query.ReviewPageView
import com.kobeinyourpocket.backend.application.tourism.query.ReviewQuery
import com.kobeinyourpocket.backend.application.tourism.query.ReviewSummaryView
import com.kobeinyourpocket.backend.application.tourism.query.ReviewView
import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.domain.report.vo.ReportStatus
import com.kobeinyourpocket.backend.domain.report.vo.ReportTarget
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId
import com.kobeinyourpocket.backend.infrastructure.query.common.JdbcTimestamps
import com.kobeinyourpocket.backend.infrastructure.query.common.JdbcUuids
import jakarta.persistence.EntityManager
import jakarta.persistence.Query
import org.springframework.stereotype.Repository

/** [ReviewQuery] の JPA 実装。スポット別・言語別でレビューを取得する。 */
@Repository
class ReviewQueryJpa(
    private val entityManager: EntityManager,
) : ReviewQuery {
    override fun findBySpot(
        spotId: SpotId,
        language: Language,
    ): List<ReviewView> {
        @Suppress("UNCHECKED_CAST")
        val rows =
            entityManager
                .createNativeQuery(
                    """
                    SELECT
                        r.id, r.spot_id, r.rating, r.comment, r.author_name, r.author_icon_url, r.author_user_id,
                        r.created_at, r.language, $HIDDEN_BY_REPORT
                    FROM review r
                    WHERE r.spot_id = :spotId AND r.language = :language
                    ORDER BY r.created_at DESC
                    """.trimIndent(),
                ).apply {
                    setParameter("spotId", spotId.value)
                    setParameter("language", language.code)
                    bindHiddenByReport()
                }.resultList as List<Array<Any?>>

        return rows.map(::toReviewView)
    }

    override fun findPage(
        page: Int,
        size: Int,
        language: Language,
    ): ReviewPageView {
        @Suppress("UNCHECKED_CAST")
        val rows =
            entityManager
                .createNativeQuery(SELECT_ALL_WITH_SPOT_NAME)
                .apply {
                    setParameter("language", language.code)
                    setParameter("fallback", Language.DEFAULT.code)
                    setParameter("limit", size)
                    setParameter("offset", page.toLong() * size)
                }.resultList as List<Array<Any?>>

        val totalElements = (entityManager.createNativeQuery(COUNT_ALL).singleResult as Number).toLong()

        return ReviewPageView(
            reviews = rows.map(::toSummaryView),
            page = page,
            size = size,
            totalElements = totalElements,
        )
    }

    /** スポット名だけ要求言語で解決する。レビュー本文・投稿者名は投稿時の言語のまま返す（#165）。 */
    private fun toSummaryView(row: Array<Any?>): ReviewSummaryView =
        ReviewSummaryView(
            id = JdbcUuids.toUuidString(row[0]),
            spotId = row[1] as String,
            spotName = row[2] as String,
            rating = (row[3] as Number).toInt(),
            comment = row[4] as String,
            authorName = row[5] as String,
            authorIconUrl = (row[6] as String).ifEmpty { null },
            createdAt = JdbcTimestamps.toInstant(row[7]),
            language = row[8] as String,
        )

    private fun toReviewView(row: Array<Any?>): ReviewView =
        ReviewView(
            id = JdbcUuids.toUuidString(row[0]),
            spotId = row[1] as String,
            rating = (row[2] as Number).toInt(),
            comment = row[3] as String,
            authorName = row[4] as String,
            authorIconUrl = (row[5] as String).ifEmpty { null },
            authorUserId = JdbcUuids.toUuidStringOrNull(row[6]),
            createdAt = JdbcTimestamps.toInstant(row[7]),
            language = row[8] as String,
            hiddenByReport = row[9] as Boolean,
        )

    private fun Query.bindHiddenByReport(): Query =
        setParameter("reportTargetType", ReportTarget.Type.REVIEW.name)
            .setParameter("resolvedReportStatus", ReportStatus.RESOLVED.name)

    private companion object {
        /**
         * 運営が通報を承認し、非表示に同意したか（`review r` を前提にした SELECT 句の 1 列）。
         * 承認は通報の状態 RESOLVED で表す（却下は DISMISSED、未対応は OPEN）。
         *
         * reports.target_id は文字列のため review.id（UUID）を文字列にして突き合わせる。
         * 一意制約 uq_reports_target_reporter (target_type, target_id, ...) の先頭 2 列が索引として効く。
         */
        val HIDDEN_BY_REPORT =
            """
            EXISTS (
                SELECT 1 FROM reports rp
                WHERE rp.target_type = :reportTargetType
                    AND rp.target_id = CAST(r.id AS VARCHAR)
                    AND rp.status = :resolvedReportStatus
            ) AS hidden_by_report
            """.trimIndent()

        /**
         * スポット名は要求言語 → en → spot_id の順で解決する（避難所一覧と同じ形）。
         *
         * spot は登録時に全言語（ja/en/zh/ko）が必須で、実データでも en 欠けは 0 件のため
         * 通常は 2 段目までで決まる。それでも `COALESCE` の最後に `r.spot_id` を置くのは、
         * 万一 en が欠けた spot があっても **null を返さない**ようにするため。
         * ここが null になると `row[2] as String` で ClassCastException になり、
         * 一覧全体が 500 になる（#158 と同じ壊れ方）。id が出れば運営は対象を特定できる。
         *
         * `LEFT JOIN` なのも同じ理由で、`INNER` にすると en 欠けの spot へのレビューが
         * 一覧から**黙って消える**。モデレーション用途では見落としの方が危険。
         *
         * created_at だけでは同時刻の行の順序が不定になり、ページ間で行が重複・欠落する。
         * 一意な id を第 2 キーに置いて全順序にする。
         */
        val SELECT_ALL_WITH_SPOT_NAME =
            """
            SELECT
                r.id,
                r.spot_id,
                COALESCE(l_req.name, l_fallback.name, r.spot_id) AS spot_name,
                r.rating,
                r.comment,
                r.author_name,
                r.author_icon_url,
                r.created_at,
                r.language
            FROM review r
            LEFT JOIN spot_localization l_req
                ON r.spot_id = l_req.spot_id AND l_req.language = :language
            LEFT JOIN spot_localization l_fallback
                ON r.spot_id = l_fallback.spot_id AND l_fallback.language = :fallback
            ORDER BY r.created_at DESC, r.id
            LIMIT :limit OFFSET :offset
            """.trimIndent()

        val COUNT_ALL =
            """
            SELECT count(*)
            FROM review
            """.trimIndent()
    }
}

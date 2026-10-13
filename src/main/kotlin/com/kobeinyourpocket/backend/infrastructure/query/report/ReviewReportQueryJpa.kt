package com.kobeinyourpocket.backend.infrastructure.query.report

import com.kobeinyourpocket.backend.application.report.query.ReportedReviewView
import com.kobeinyourpocket.backend.application.report.query.ReviewReportGroupView
import com.kobeinyourpocket.backend.application.report.query.ReviewReportItemView
import com.kobeinyourpocket.backend.application.report.query.ReviewReportPageView
import com.kobeinyourpocket.backend.application.report.query.ReviewReportQuery
import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.domain.report.vo.ReportStatus
import com.kobeinyourpocket.backend.domain.report.vo.ReportTarget
import com.kobeinyourpocket.backend.infrastructure.query.common.JdbcTimestamps
import com.kobeinyourpocket.backend.infrastructure.query.common.JdbcUuids
import jakarta.persistence.EntityManager
import jakarta.persistence.Query
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

/**
 * [ReviewReportQuery] の JPA 実装（#145）。
 *
 * 3 本のクエリで組み立てる。いずれもページに出る口コミの分だけを読むため、件数が増えても N+1 にならない。
 * 1. 口コミ単位の集計（並び替え・ページング）
 * 2. そのページの口コミへの通報の明細（通報者名つき）
 * 3. そのページの口コミの中身（スポット名は要求言語で解決）
 */
@Repository
class ReviewReportQueryJpa(
    private val entityManager: EntityManager,
) : ReviewReportQuery {
    override fun findPage(
        status: ReportStatus?,
        page: Int,
        size: Int,
        language: Language,
    ): ReviewReportPageView {
        val having = if (status == null) "" else HAVING_STATUS
        val summaries =
            entityManager
                .createNativeQuery(SELECT_GROUPS.format(having))
                .setParameter("targetType", REVIEW)
                .setParameter("open", ReportStatus.OPEN.name)
                .bindStatus(status)
                .setParameter("limit", size)
                .setParameter("offset", page.toLong() * size)
                .resultRows()
                .map(::toSummary)
        val totalElements =
            (
                entityManager
                    .createNativeQuery(COUNT_GROUPS.format(having))
                    .setParameter("targetType", REVIEW)
                    .bindStatus(status)
                    .singleResult as Number
            ).toLong()

        val targetIds = summaries.map(Summary::targetId)
        val reportsByTarget = findReports(targetIds).groupBy(Pair<String, ReviewReportItemView>::first, { it.second })
        val reviewsById = findReviews(targetIds, language)

        val groups =
            summaries.map { summary ->
                val reports = reportsByTarget[summary.targetId].orEmpty()
                ReviewReportGroupView(
                    reviewId = summary.targetId,
                    review = reviewsById[summary.targetId],
                    reportCount = summary.reportCount,
                    openCount = summary.openCount,
                    reasonCounts = reports.groupingBy(ReviewReportItemView::reason).eachCount(),
                    latestReportedAt = summary.latestReportedAt,
                    reports = reports,
                )
            }
        return ReviewReportPageView(groups = groups, page = page, size = size, totalElements = totalElements)
    }

    /** @return (target_id, 通報) の組。新しい順。 */
    private fun findReports(targetIds: List<String>): List<Pair<String, ReviewReportItemView>> {
        if (targetIds.isEmpty()) return emptyList()
        return entityManager
            .createNativeQuery(SELECT_REPORTS)
            .setParameter("targetType", REVIEW)
            .setParameter("targetIds", targetIds)
            .resultRows()
            .map { row ->
                row[1] as String to
                    ReviewReportItemView(
                        id = JdbcUuids.toUuidString(row[0]),
                        reason = row[2] as String,
                        description = row[3] as String?,
                        status = row[4] as String,
                        reporterUserId = JdbcUuids.toUuidString(row[5]),
                        reporterName = row[6] as String?,
                        createdAt = JdbcTimestamps.toInstant(row[7]),
                        handledBy = JdbcUuids.toUuidStringOrNull(row[8]),
                        handledAt = row[9]?.let(JdbcTimestamps::toInstant),
                    )
            }
    }

    /**
     * 口コミの中身を id で引く。削除済みの口コミは結果に含まれず、呼び出し側で null になる。
     *
     * target_id は文字列だが REVIEW は必ず UUID なので、UUID に直して主キーで引く
     * （`review.id::text` で比較すると主キーの索引が効かない）。
     */
    private fun findReviews(
        targetIds: List<String>,
        language: Language,
    ): Map<String, ReportedReviewView> {
        val reviewIds = targetIds.mapNotNull { runCatching { UUID.fromString(it) }.getOrNull() }
        if (reviewIds.isEmpty()) return emptyMap()
        return entityManager
            .createNativeQuery(SELECT_REVIEWS)
            .setParameter("reviewIds", reviewIds)
            .setParameter("language", language.code)
            .setParameter("fallback", Language.DEFAULT.code)
            .resultRows()
            .associate { row ->
                JdbcUuids.toUuidString(row[0]) to
                    ReportedReviewView(
                        spotId = row[1] as String,
                        spotName = row[2] as String,
                        rating = (row[3] as Number).toInt(),
                        comment = row[4] as String,
                        authorUserId = JdbcUuids.toUuidStringOrNull(row[5]),
                        authorName = row[6] as String,
                        language = row[7] as String,
                        postedAt = JdbcTimestamps.toInstant(row[8]),
                    )
            }
    }

    private fun Query.bindStatus(status: ReportStatus?): Query = if (status == null) this else setParameter("status", status.name)

    @Suppress("UNCHECKED_CAST")
    private fun Query.resultRows(): List<Array<Any?>> = resultList as List<Array<Any?>>

    private fun toSummary(row: Array<Any?>): Summary =
        Summary(
            targetId = row[0] as String,
            reportCount = (row[1] as Number).toInt(),
            openCount = (row[2] as Number).toInt(),
            latestReportedAt = JdbcTimestamps.toInstant(row[3]),
        )

    private data class Summary(
        val targetId: String,
        val reportCount: Int,
        val openCount: Int,
        val latestReportedAt: Instant,
    )

    private companion object {
        val REVIEW = ReportTarget.Type.REVIEW.name

        /** 指定した状態の通報が 1 件以上ある口コミだけに絞る。集計値（件数・内訳）は全通報のまま。 */
        const val HAVING_STATUS = "HAVING SUM(CASE WHEN r.status = :status THEN 1 ELSE 0 END) > 0"

        /**
         * 口コミ単位の集計。`%s` に [HAVING_STATUS]（絞り込み無しなら空文字）を差し込む。
         *
         * 未対応が多い順 → 最新の通報が新しい順 → target_id の順で全順序にする
         * （同順位の行の並びが不定だと、ページ間で行が重複・欠落する）。
         * FILTER 句ではなく SUM(CASE) にしているのは H2（テスト）でも同じ SQL を通すため。
         */
        val SELECT_GROUPS =
            """
            SELECT
                r.target_id,
                COUNT(*) AS report_count,
                SUM(CASE WHEN r.status = :open THEN 1 ELSE 0 END) AS open_count,
                MAX(r.created_at) AS latest_reported_at
            FROM reports r
            WHERE r.target_type = :targetType
            GROUP BY r.target_id
            %s
            ORDER BY open_count DESC, latest_reported_at DESC, r.target_id
            LIMIT :limit OFFSET :offset
            """.trimIndent()

        val COUNT_GROUPS =
            """
            SELECT COUNT(*) FROM (
                SELECT r.target_id
                FROM reports r
                WHERE r.target_type = :targetType
                GROUP BY r.target_id
                %s
            ) g
            """.trimIndent()

        /** 通報者の表示名は users から引く。プロフィール行が無ければ null（LEFT JOIN）。 */
        val SELECT_REPORTS =
            """
            SELECT
                r.id,
                r.target_id,
                r.reason,
                r.description,
                r.status,
                r.reporter_user_id,
                u.name,
                r.created_at,
                r.handled_by,
                r.handled_at
            FROM reports r
            LEFT JOIN users u ON u.id = r.reporter_user_id
            WHERE r.target_type = :targetType AND r.target_id IN (:targetIds)
            ORDER BY r.created_at DESC, r.id
            """.trimIndent()

        /** スポット名は要求言語 → en → spot_id の順で解決する（運営向け口コミ一覧と同じ / ReviewQueryJpa）。 */
        val SELECT_REVIEWS =
            """
            SELECT
                v.id,
                v.spot_id,
                COALESCE(l_req.name, l_fallback.name, v.spot_id) AS spot_name,
                v.rating,
                v.comment,
                v.author_user_id,
                v.author_name,
                v.language,
                v.created_at
            FROM review v
            LEFT JOIN spot_localization l_req
                ON v.spot_id = l_req.spot_id AND l_req.language = :language
            LEFT JOIN spot_localization l_fallback
                ON v.spot_id = l_fallback.spot_id AND l_fallback.language = :fallback
            WHERE v.id IN (:reviewIds)
            """.trimIndent()
    }
}

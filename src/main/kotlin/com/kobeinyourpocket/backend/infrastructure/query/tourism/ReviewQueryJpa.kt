package com.kobeinyourpocket.backend.infrastructure.query.tourism

import com.kobeinyourpocket.backend.application.tourism.query.ReviewPageView
import com.kobeinyourpocket.backend.application.tourism.query.ReviewQuery
import com.kobeinyourpocket.backend.application.tourism.query.ReviewSummaryView
import com.kobeinyourpocket.backend.application.tourism.query.ReviewView
import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId
import com.kobeinyourpocket.backend.infrastructure.query.common.JdbcTimestamps
import jakarta.persistence.EntityManager
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
                    SELECT id, spot_id, rating, comment, author_name, author_icon_url, created_at, language
                    FROM review
                    WHERE spot_id = :spotId AND language = :language
                    ORDER BY created_at DESC
                    """.trimIndent(),
                ).apply {
                    setParameter("spotId", spotId.value)
                    setParameter("language", language.code)
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
            id = row[0].toString(),
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
            id = row[0].toString(),
            spotId = row[1] as String,
            rating = (row[2] as Number).toInt(),
            comment = row[3] as String,
            authorName = row[4] as String,
            authorIconUrl = (row[5] as String).ifEmpty { null },
            createdAt = JdbcTimestamps.toInstant(row[6]),
            language = row[7] as String,
        )

    private companion object {
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

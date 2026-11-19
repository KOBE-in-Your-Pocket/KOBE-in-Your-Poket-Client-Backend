package com.kobeinyourpocket.backend.infrastructure.query.tourism

import com.kobeinyourpocket.backend.application.tourism.query.SpotViewCount
import com.kobeinyourpocket.backend.application.tourism.query.SpotViewCountQuery
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository

/** [SpotViewCountQuery] の JPA 実装（#239）。 */
@Repository
class SpotViewCountQueryJpa(
    private val entityManager: EntityManager,
) : SpotViewCountQuery {
    override fun countBySpotId(spotId: SpotId): Long =
        toLong(
            entityManager
                .createNativeQuery(COUNT_BY_SPOT_ID)
                .setParameter("spotId", spotId.value)
                .singleResult,
        )

    override fun countAllBySpot(): List<SpotViewCount> {
        @Suppress("UNCHECKED_CAST")
        val rows =
            entityManager
                .createNativeQuery(COUNT_ALL_BY_SPOT)
                .resultList as List<Array<Any?>>

        return rows.map { row ->
            SpotViewCount(
                spotId = row[Column.SPOT_ID] as String,
                viewCount = toLong(row[Column.VIEW_COUNT]),
            )
        }
    }

    /** COUNT の型は JDBC ドライバで変わる（PostgreSQL は Long、H2 は Long / BigInteger など）ため、Number で受ける。 */
    private fun toLong(value: Any?): Long = (value as Number).toLong()

    /** [COUNT_ALL_BY_SPOT] の列順と対応する index。列の並び替え時は両方を合わせて更新すること。 */
    private object Column {
        const val SPOT_ID = 0
        const val VIEW_COUNT = 1
    }

    private companion object {
        val COUNT_BY_SPOT_ID =
            """
            SELECT COUNT(*)
            FROM spot_views v
            WHERE v.spot_id = :spotId
            """.trimIndent()

        /**
         * `spot` と `INNER JOIN` するのは、削除済みのスポットを一覧に出さないため。
         * 本番では外部キー（V26 の `ON DELETE CASCADE`）で閲覧も消えるが、
         * 外部キーの無い環境（テストの H2）でも同じ結果になるよう、クエリでも絞る（FavoriteQueryJpa と同じ）。
         */
        val COUNT_ALL_BY_SPOT =
            """
            SELECT v.spot_id, COUNT(*) AS view_count
            FROM spot_views v
            JOIN spot s ON s.id = v.spot_id
            GROUP BY v.spot_id
            ORDER BY view_count DESC, v.spot_id
            """.trimIndent()
    }
}

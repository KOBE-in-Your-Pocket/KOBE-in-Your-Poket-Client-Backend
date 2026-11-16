package com.kobeinyourpocket.backend.infrastructure.query.tourism

import com.kobeinyourpocket.backend.application.tourism.query.FavoriteQuery
import com.kobeinyourpocket.backend.application.tourism.query.FavoriteView
import com.kobeinyourpocket.backend.domain.tourism.favorite.vo.FavoriteOwnerId
import com.kobeinyourpocket.backend.infrastructure.query.common.JdbcTimestamps
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository

/** [FavoriteQuery] の JPA 実装（#237）。 */
@Repository
class FavoriteQueryJpa(
    private val entityManager: EntityManager,
) : FavoriteQuery {
    override fun findByOwner(ownerId: FavoriteOwnerId): List<FavoriteView> {
        @Suppress("UNCHECKED_CAST")
        val rows =
            entityManager
                .createNativeQuery(SELECT_FAVORITES)
                .setParameter("ownerId", ownerId.value)
                .resultList as List<Array<Any?>>

        return rows.map(::toView)
    }

    private fun toView(row: Array<Any?>): FavoriteView =
        FavoriteView(
            spotId = row[Column.SPOT_ID] as String,
            createdAt = JdbcTimestamps.toInstant(row[Column.CREATED_AT]),
        )

    /** [SELECT_FAVORITES] の列順と対応する index。列の並び替え時は両方を合わせて更新すること。 */
    private object Column {
        const val SPOT_ID = 0
        const val CREATED_AT = 1
    }

    private companion object {
        /**
         * `spot` と `INNER JOIN` するのは、削除済みのスポットを一覧に出さないため。
         * 本番では外部キー（V25 の `ON DELETE CASCADE`）でお気に入りも消えるが、
         * 外部キーの無い環境（テストの H2）でも同じ結果になるよう、クエリでも絞る。
         *
         * 同時刻の行の順序を固定するため、一意な spot_id を第 2 キーに置く。
         */
        val SELECT_FAVORITES =
            """
            SELECT f.spot_id, f.created_at
            FROM favorites f
            JOIN spot s ON s.id = f.spot_id
            WHERE f.user_id = :ownerId
            ORDER BY f.created_at DESC, f.spot_id
            """.trimIndent()
    }
}

package com.kobeinyourpocket.backend.infrastructure.query.tourism

import com.kobeinyourpocket.backend.application.tourism.query.GenreQuery
import com.kobeinyourpocket.backend.application.tourism.query.GenreView
import com.kobeinyourpocket.backend.domain.tourism.genre.vo.GenreCode
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository

/** [GenreQuery] の JPA 実装（#153）。 */
@Repository
class GenreQueryJpa(
    private val entityManager: EntityManager,
) : GenreQuery {
    override fun findAll(): List<GenreView> {
        @Suppress("UNCHECKED_CAST")
        val rows =
            entityManager
                .createNativeQuery(SELECT_GENRES)
                .resultList as List<Array<Any?>>

        // 言語ごとに 1 行返るため、code でまとめて 1 件の View にする。
        // LinkedHashMap で SQL の並び順（display_order → code）を保つ。
        val byCode = LinkedHashMap<String, MutableList<Array<Any?>>>()
        rows.forEach { row ->
            byCode.getOrPut(row[Column.CODE] as String) { mutableListOf() }.add(row)
        }

        return byCode.map { (code, group) ->
            val first = group.first()
            GenreView(
                code = code,
                displayOrder = (first[Column.DISPLAY_ORDER] as Number).toInt(),
                labels = group.associate { (it[Column.LANGUAGE] as String) to (it[Column.LABEL] as String) },
                spotCount = (first[Column.SPOT_COUNT] as Number).toLong(),
            )
        }
    }

    override fun countSpotsByGenre(code: GenreCode): Long =
        (
            entityManager
                .createNativeQuery(COUNT_SPOTS_BY_GENRE)
                .setParameter("code", code.value)
                .singleResult as Number
        ).toLong()

    /** [SELECT_GENRES] の列順と対応する index。列の並び替え時は両方を合わせて更新すること。 */
    private object Column {
        const val CODE = 0
        const val DISPLAY_ORDER = 1
        const val SPOT_COUNT = 2
        const val LANGUAGE = 3
        const val LABEL = 4
    }

    private companion object {
        /**
         * ジャンルと全言語のラベルを 1 往復で取る。件数が少ない（運営が管理するマスタ）ため、
         * 言語ごとの行を展開してもコストにならない。
         *
         * spot 件数は相関サブクエリで数える。`INNER JOIN spot` にすると、使われていない
         * ジャンルが一覧から**消えてしまう**（0 件のジャンルこそ削除候補として見せたい）。
         *
         * 並びは display_order → code。display_order が同値のときの順序が不定にならないよう
         * code を第 2 キーに置く。言語は language 順に固定して、行の並びを安定させる。
         */
        val SELECT_GENRES =
            """
            SELECT
                g.code,
                g.display_order,
                (SELECT count(*) FROM spot s WHERE s.genre = g.code) AS spot_count,
                l.language,
                l.label
            FROM genre g
            JOIN genre_localization l ON l.genre_code = g.code
            ORDER BY g.display_order, g.code, l.language
            """.trimIndent()

        val COUNT_SPOTS_BY_GENRE =
            """
            SELECT count(*)
            FROM spot
            WHERE genre = :code
            """.trimIndent()
    }
}

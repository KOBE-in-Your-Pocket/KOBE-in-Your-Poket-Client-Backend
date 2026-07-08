package com.kobeinyourpocket.backend.infrastructure.query.manner

import com.kobeinyourpocket.backend.application.manner.query.MannerItemQuery
import com.kobeinyourpocket.backend.application.manner.query.MannerItemView
import com.kobeinyourpocket.backend.domain.common.localization.Language
import jakarta.persistence.EntityManager
import jakarta.persistence.Query
import org.springframework.stereotype.Repository

/**
 * [MannerItemQuery] の JPA 実装。要求言語 + フォールバック（既定 en / [Language.DEFAULT]）を SQL で解決し projection を返す。
 *
 * relatedSpotIds は M-2 の ID 参照のみ（JOIN 配信しない）。行の複製を避けるため
 * `manner_item_spot` は別クエリで引き、メモリ上で項目 id ごとにまとめる。
 */
@Repository
class MannerItemQueryJpa(
    private val entityManager: EntityManager,
) : MannerItemQuery {
    override fun findAllResolved(language: Language): List<MannerItemView> {
        val relatedSpotIdsByItemId = findRelatedSpotIds()
        return entityManager
            .createNativeQuery(SELECT_RESOLVED_MANNER_ITEM)
            .setParameter("language", language.code)
            .setParameter("fallback", Language.DEFAULT.code)
            .resultRows()
            .map { row -> toMannerItemView(row, relatedSpotIdsByItemId) }
    }

    private fun findRelatedSpotIds(): Map<String, List<String>> =
        entityManager
            .createNativeQuery(SELECT_RELATED_SPOT_IDS)
            .resultRows()
            .groupBy(keySelector = { it[0] as String }, valueTransform = { it[1] as String })

    @Suppress("UNCHECKED_CAST")
    private fun Query.resultRows(): List<Array<Any?>> = resultList as List<Array<Any?>>

    private fun toMannerItemView(
        row: Array<Any?>,
        relatedSpotIdsByItemId: Map<String, List<String>>,
    ): MannerItemView {
        val id = row[0] as String
        return MannerItemView(
            id = id,
            title = row[1] as String,
            description = row[2] as String,
            icon = row[3] as String,
            kind = row[4] as String,
            scope = row[5] as String,
            relatedSpotIds = relatedSpotIdsByItemId[id].orEmpty(),
        )
    }

    private companion object {
        val SELECT_RESOLVED_MANNER_ITEM =
            """
            SELECT
                m.id,
                COALESCE(l_req.title, l_fallback.title) AS title,
                COALESCE(l_req.description, l_fallback.description) AS description,
                m.icon,
                m.kind,
                m.scope
            FROM manner_item m
            LEFT JOIN manner_item_localization l_req
                ON m.id = l_req.manner_item_id AND l_req.language = :language
            LEFT JOIN manner_item_localization l_fallback
                ON m.id = l_fallback.manner_item_id AND l_fallback.language = :fallback
            ORDER BY m.id
            """.trimIndent()

        val SELECT_RELATED_SPOT_IDS =
            """
            SELECT s.manner_item_id, s.spot_id
            FROM manner_item_spot s
            ORDER BY s.manner_item_id, s.spot_id
            """.trimIndent()
    }
}

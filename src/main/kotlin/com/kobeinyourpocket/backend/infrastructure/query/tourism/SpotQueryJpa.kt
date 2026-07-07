package com.kobeinyourpocket.backend.infrastructure.query.tourism

import com.kobeinyourpocket.backend.application.tourism.query.SpotQuery
import com.kobeinyourpocket.backend.application.tourism.query.SpotView
import com.kobeinyourpocket.backend.domain.tourism.vo.Language
import com.kobeinyourpocket.backend.domain.tourism.vo.SpotId
import jakarta.persistence.EntityManager
import jakarta.persistence.Query
import org.springframework.stereotype.Repository

/**
 * [SpotQuery] の JPA 実装。要求言語 + ja フォールバックを SQL で解決し projection を返す。
 */
@Repository
class SpotQueryJpa(
    private val entityManager: EntityManager,
) : SpotQuery {
    override fun findAllResolved(language: Language): List<SpotView> =
        resolvedQuery(language, "$SELECT_RESOLVED_SPOT ORDER BY s.id").resultRows().map(::toSpotView)

    override fun findByIdResolved(
        id: SpotId,
        language: Language,
    ): SpotView? =
        resolvedQuery(language, "$SELECT_RESOLVED_SPOT WHERE s.id = :id")
            .setParameter("id", id.value)
            .setMaxResults(1)
            .resultRows()
            .map(::toSpotView)
            .firstOrNull()

    private fun resolvedQuery(
        language: Language,
        sql: String,
    ): Query =
        entityManager
            .createNativeQuery(sql)
            .setParameter("language", language.code)
            .setParameter("fallback", Language.DEFAULT.code)

    @Suppress("UNCHECKED_CAST")
    private fun Query.resultRows(): List<Array<Any?>> = resultList as List<Array<Any?>>

    private fun toSpotView(row: Array<Any?>): SpotView =
        SpotView(
            id = row[0] as String,
            name = row[1] as String,
            genre = row[2] as String,
            description = row[3] as String,
            latitude = (row[4] as Number).toDouble(),
            longitude = (row[5] as Number).toDouble(),
            businessHours = row[6] as String,
            categoryLabel = row[7] as String,
            imageUrl = row[8] as String,
            rating = (row[9] as Number?)?.toDouble(),
            address = row[10] as String,
        )

    private companion object {
        val SELECT_RESOLVED_SPOT =
            """
            SELECT
                s.id,
                COALESCE(l_req.name, l_ja.name) AS name,
                s.genre,
                COALESCE(l_req.description, l_ja.description) AS description,
                s.latitude,
                s.longitude,
                COALESCE(l_req.business_hours, l_ja.business_hours) AS business_hours,
                COALESCE(l_req.category_label, l_ja.category_label) AS category_label,
                s.image_url,
                (SELECT AVG(r.rating) FROM review r WHERE r.spot_id = s.id) AS rating_value,
                COALESCE(l_req.address, l_ja.address) AS address
            FROM spot s
            LEFT JOIN spot_localization l_req
                ON s.id = l_req.spot_id AND l_req.language = :language
            LEFT JOIN spot_localization l_ja
                ON s.id = l_ja.spot_id AND l_ja.language = :fallback
            """.trimIndent()
    }
}

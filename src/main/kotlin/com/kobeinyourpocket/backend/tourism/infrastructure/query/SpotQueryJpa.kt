package com.kobeinyourpocket.backend.tourism.infrastructure.query

import com.kobeinyourpocket.backend.tourism.application.query.SpotQuery
import com.kobeinyourpocket.backend.tourism.application.query.SpotView
import com.kobeinyourpocket.backend.tourism.domain.vo.Language
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository

/**
 * [SpotQuery] の JPA 実装。要求言語 + ja フォールバックを SQL で解決し projection を返す。
 */
@Repository
class SpotQueryJpa(
    private val entityManager: EntityManager,
) : SpotQuery {
    override fun findAllResolved(language: Language): List<SpotView> {
        @Suppress("UNCHECKED_CAST")
        val rows =
            entityManager
                .createNativeQuery(
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
                        s.rating_value
                    FROM spot s
                    LEFT JOIN spot_localization l_req
                        ON s.id = l_req.spot_id AND l_req.language = :language
                    LEFT JOIN spot_localization l_ja
                        ON s.id = l_ja.spot_id AND l_ja.language = :fallback
                    ORDER BY s.id
                    """.trimIndent(),
                ).apply {
                    setParameter("language", language.code)
                    setParameter("fallback", Language.DEFAULT.code)
                }.resultList as List<Array<Any?>>

        return rows.map(::toSpotView)
    }

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
        )
}

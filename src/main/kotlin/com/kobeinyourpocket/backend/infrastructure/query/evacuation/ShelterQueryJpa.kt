package com.kobeinyourpocket.backend.infrastructure.query.evacuation

import com.kobeinyourpocket.backend.application.evacuation.query.ShelterQuery
import com.kobeinyourpocket.backend.application.evacuation.query.ShelterView
import com.kobeinyourpocket.backend.domain.common.localization.Language
import jakarta.persistence.EntityManager
import jakarta.persistence.Query
import org.springframework.stereotype.Repository

/**
 * [ShelterQuery] の JPA 実装。要求言語 + フォールバック（既定 en / [Language.DEFAULT]）を SQL で解決し projection を返す。
 */
@Repository
class ShelterQueryJpa(
    private val entityManager: EntityManager,
) : ShelterQuery {
    override fun findAllResolved(language: Language): List<ShelterView> =
        entityManager
            .createNativeQuery(SELECT_RESOLVED_SHELTER)
            .setParameter("language", language.code)
            .setParameter("fallback", Language.DEFAULT.code)
            .resultRows()
            .map(::toShelterView)

    @Suppress("UNCHECKED_CAST")
    private fun Query.resultRows(): List<Array<Any?>> = resultList as List<Array<Any?>>

    private fun toShelterView(row: Array<Any?>): ShelterView =
        ShelterView(
            id = row[0] as String,
            name = row[1] as String,
            address = row[2] as String,
            latitude = (row[3] as Number).toDouble(),
            longitude = (row[4] as Number).toDouble(),
            type = row[5] as String,
            facilityCategory = row[6] as String,
            imageUrl = row[7] as String,
            capacity = (row[8] as Number?)?.toInt(),
            accessible = row[9] as Boolean,
            externalUrl = row[10] as String?,
        )

    private companion object {
        val SELECT_RESOLVED_SHELTER =
            """
            SELECT
                s.id,
                COALESCE(l_req.name, l_fallback.name) AS name,
                COALESCE(l_req.address, l_fallback.address) AS address,
                s.latitude,
                s.longitude,
                s.type,
                s.facility_category,
                s.image_url,
                s.capacity,
                s.accessible,
                s.external_url
            FROM shelter s
            LEFT JOIN shelter_localization l_req
                ON s.id = l_req.shelter_id AND l_req.language = :language
            LEFT JOIN shelter_localization l_fallback
                ON s.id = l_fallback.shelter_id AND l_fallback.language = :fallback
            ORDER BY s.id
            """.trimIndent()
    }
}

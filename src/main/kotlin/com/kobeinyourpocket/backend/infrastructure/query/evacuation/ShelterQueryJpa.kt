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
            id = row[Column.ID] as String,
            name = row[Column.NAME] as String,
            address = row[Column.ADDRESS] as String,
            latitude = (row[Column.LATITUDE] as Number).toDouble(),
            longitude = (row[Column.LONGITUDE] as Number).toDouble(),
            type = row[Column.TYPE] as String,
            facilityCategory = row[Column.FACILITY_CATEGORY] as String,
            imageUrl = row[Column.IMAGE_URL] as String,
            capacity = (row[Column.CAPACITY] as Number?)?.toInt(),
            accessible = row[Column.ACCESSIBLE] as Boolean,
            externalUrl = row[Column.EXTERNAL_URL] as String?,
        )

    /** [SELECT_RESOLVED_SHELTER] の列順と対応する index。列の並び替え時は両方を合わせて更新すること。 */
    private object Column {
        const val ID = 0
        const val NAME = 1
        const val ADDRESS = 2
        const val LATITUDE = 3
        const val LONGITUDE = 4
        const val TYPE = 5
        const val FACILITY_CATEGORY = 6
        const val IMAGE_URL = 7
        const val CAPACITY = 8
        const val ACCESSIBLE = 9
        const val EXTERNAL_URL = 10
    }

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

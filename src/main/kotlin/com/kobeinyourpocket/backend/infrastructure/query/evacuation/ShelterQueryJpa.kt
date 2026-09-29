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
            siting = row[Column.SITING] as String,
            suitabilityLandslide = row[Column.SUITABILITY_LANDSLIDE] as String,
            suitabilityFlood = row[Column.SUITABILITY_FLOOD] as String,
            suitabilityTsunami = row[Column.SUITABILITY_TSUNAMI] as String,
            suitabilityLargeFire = row[Column.SUITABILITY_LARGE_FIRE] as String,
            petAcceptance = row[Column.PET_ACCEPTANCE] as String,
            phoneNumber = row[Column.PHONE_NUMBER] as String?,
            note = row[Column.NOTE] as String?,
        )

    /** [SELECT_RESOLVED_SHELTER] の列順と対応する index。列の並び替え時は両方を合わせて更新すること。 */
    private object Column {
        const val ID = 0
        const val NAME = 1
        const val ADDRESS = 2
        const val NOTE = 3
        const val LATITUDE = 4
        const val LONGITUDE = 5
        const val TYPE = 6
        const val SITING = 7
        const val SUITABILITY_LANDSLIDE = 8
        const val SUITABILITY_FLOOD = 9
        const val SUITABILITY_TSUNAMI = 10
        const val SUITABILITY_LARGE_FIRE = 11
        const val PET_ACCEPTANCE = 12
        const val PHONE_NUMBER = 13
    }

    private companion object {
        val SELECT_RESOLVED_SHELTER =
            """
            SELECT
                s.id,
                COALESCE(l_req.name, l_fallback.name) AS name,
                COALESCE(l_req.address, l_fallback.address) AS address,
                COALESCE(l_req.note, l_fallback.note) AS note,
                s.latitude,
                s.longitude,
                s.type,
                s.siting,
                s.suitability_landslide,
                s.suitability_flood,
                s.suitability_tsunami,
                s.suitability_large_fire,
                s.pet_acceptance,
                s.phone_number
            FROM shelter s
            LEFT JOIN shelter_localization l_req
                ON s.id = l_req.shelter_id AND l_req.language = :language
            LEFT JOIN shelter_localization l_fallback
                ON s.id = l_fallback.shelter_id AND l_fallback.language = :fallback
            ORDER BY s.id
            """.trimIndent()
    }
}

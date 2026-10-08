package com.kobeinyourpocket.backend.infrastructure.query.evacuation

import com.kobeinyourpocket.backend.application.evacuation.query.ShelterQuery
import com.kobeinyourpocket.backend.application.evacuation.query.ShelterView
import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.DisasterType
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterSuitability
import jakarta.persistence.EntityManager
import jakarta.persistence.Query
import org.springframework.stereotype.Repository

/**
 * [ShelterQuery] の JPA 実装。要求言語 + フォールバック（既定 en / [Language.DEFAULT]）を SQL で解決し projection を返す。
 *
 * 災害種別での絞り込みは WHERE 句で行う。列名は [DisasterType] から固定の対応表で引くため、
 * 利用者の入力が SQL に入ることはない（値はバインドパラメータ）。
 */
@Repository
class ShelterQueryJpa(
    private val entityManager: EntityManager,
) : ShelterQuery {
    override fun findAllResolved(
        language: Language,
        suitableFor: Set<DisasterType>,
    ): List<ShelterView> {
        val query =
            entityManager
                .createNativeQuery(selectResolvedShelter(suitableFor))
                .setParameter("language", language.code)
                .setParameter("fallback", Language.DEFAULT.code)
        if (suitableFor.isNotEmpty()) query.setParameter("suitable", ShelterSuitability.SUITABLE.wireValue)
        return query.resultRows().map(::toShelterView)
    }

    /** 種別が空なら WHERE 無し（全件）。種別を並べる順は enum 順に固定し、同じ条件で同じ SQL にする。 */
    private fun selectResolvedShelter(suitableFor: Set<DisasterType>): String {
        if (suitableFor.isEmpty()) return SELECT_RESOLVED_SHELTER.format("")
        val conditions =
            DisasterType.entries
                .filter(suitableFor::contains)
                .joinToString(" OR ") { "s.${suitabilityColumn(it)} = :suitable" }
        return SELECT_RESOLVED_SHELTER.format("WHERE $conditions")
    }

    private fun suitabilityColumn(disasterType: DisasterType): String =
        when (disasterType) {
            DisasterType.LANDSLIDE -> "suitability_landslide"
            DisasterType.FLOOD -> "suitability_flood"
            DisasterType.TSUNAMI -> "suitability_tsunami"
            DisasterType.LARGE_FIRE -> "suitability_large_fire"
        }

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
        /** `%s` に WHERE 句（絞り込み無しなら空文字）を差し込む。 */
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
            %s
            ORDER BY s.id
            """.trimIndent()
    }
}

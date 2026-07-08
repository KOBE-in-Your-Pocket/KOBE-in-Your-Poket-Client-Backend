package com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.model

import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterCapacity
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterCoordinates
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterExternalUrl
import com.kobeinyourpocket.backend.domain.evacuation.shelterfacilitycategory.model.ShelterFacilityCategory
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterLocalizations
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterMedia
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterType

/**
 * 避難所エンティティ（集約ルート）。
 *
 * 「避難所 1 件」を表す。同一性は [Id] が担い、内容が更新されても同じ EvacuationShelter として続く。
 * 全言語のローカライズを [localizations] として所有し、集約の整合境界に含める。
 *
 * Client `EvacuationShelter` のうち id / coordinates / type / facilityCategory /
 * media / capacity? / accessible / externalUrl? に相当する（name / address は [localizations]）。
 */
data class EvacuationShelter(
    val id: Id,
    val coordinates: ShelterCoordinates,
    val type: ShelterType,
    val facilityCategory: ShelterFacilityCategory,
    val media: ShelterMedia,
    val accessible: Boolean,
    val localizations: ShelterLocalizations,
    val capacity: ShelterCapacity? = null,
    val externalUrl: ShelterExternalUrl? = null,
) {
    /**
     * 避難所の識別子（値オブジェクト）。
     *
     * エンティティの同一性そのものであり、エンティティを構成するため本ファイルに同居させる。
     * [Companion.of] が生成入口。
     */
    @JvmInline
    value class Id private constructor(
        val value: String,
    ) {
        init {
            require(value.isNotBlank()) { "EvacuationShelter.Id must not be blank" }
            require(value.length <= MAX_LENGTH) {
                "EvacuationShelter.Id must be at most $MAX_LENGTH characters, got ${value.length}"
            }
        }

        override fun toString(): String = value

        companion object {
            private const val MAX_LENGTH = 128

            fun of(value: String): Id = Id(value.trim())
        }
    }

    companion object {
        fun create(
            id: Id,
            coordinates: ShelterCoordinates,
            type: ShelterType,
            facilityCategory: ShelterFacilityCategory,
            media: ShelterMedia,
            accessible: Boolean,
            localizations: ShelterLocalizations,
            capacity: ShelterCapacity? = null,
            externalUrl: String? = null,
        ): EvacuationShelter =
            EvacuationShelter(
                id = id,
                coordinates = coordinates,
                type = type,
                facilityCategory = facilityCategory,
                media = media,
                accessible = accessible,
                localizations = localizations,
                capacity = capacity,
                externalUrl = ShelterExternalUrl.of(externalUrl),
            )
    }
}

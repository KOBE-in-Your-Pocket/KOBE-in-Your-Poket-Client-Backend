package com.kobeinyourpocket.backend.domain.evacuation.aggregate

import com.kobeinyourpocket.backend.domain.evacuation.vo.ShelterCapacity
import com.kobeinyourpocket.backend.domain.evacuation.vo.ShelterCoordinates
import com.kobeinyourpocket.backend.domain.evacuation.vo.ShelterFacilityCategory
import com.kobeinyourpocket.backend.domain.evacuation.vo.ShelterId
import com.kobeinyourpocket.backend.domain.evacuation.vo.ShelterMedia
import com.kobeinyourpocket.backend.domain.evacuation.vo.ShelterType

/**
 * 避難所集約ルート（Entity・言語非依存部分）。
 *
 * `vo` パッケージの Value Object をコンポジションで保持する。
 * 同一 [ShelterId] により識別され、中身の更新は同じ EvacuationShelter として扱う。
 *
 * 言語依存フィールド（name / address）は #64 ShelterLocalization で扱う。
 * API 返却形への合成も application 層で行う。
 *
 * Client `EvacuationShelter` のうち id / coordinates / type / facilityCategory /
 * media / capacity? / accessible / externalUrl? に相当する。
 */
data class EvacuationShelter(
    val id: ShelterId,
    val coordinates: ShelterCoordinates,
    val type: ShelterType,
    val facilityCategory: ShelterFacilityCategory,
    val media: ShelterMedia,
    val accessible: Boolean,
    val capacity: ShelterCapacity? = null,
    val externalUrl: String? = null,
) {
    init {
        externalUrl?.let { url ->
            require(url.isNotBlank()) { "externalUrl must not be blank when provided" }
        }
    }

    companion object {
        fun create(
            id: ShelterId,
            coordinates: ShelterCoordinates,
            type: ShelterType,
            facilityCategory: ShelterFacilityCategory,
            media: ShelterMedia,
            accessible: Boolean,
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
                capacity = capacity,
                externalUrl = externalUrl,
            )
    }
}

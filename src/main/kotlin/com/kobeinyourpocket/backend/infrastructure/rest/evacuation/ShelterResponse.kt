package com.kobeinyourpocket.backend.infrastructure.rest.evacuation

import com.fasterxml.jackson.annotation.JsonInclude
import com.kobeinyourpocket.backend.application.evacuation.query.ShelterView

/**
 * `GET /api/v1/evacuation/shelters` のレスポンス（Client `EvacuationShelter` 形に一致 / 要件定義 D1）。
 *
 * type は `emergency|designated|both` のリテラル（Client `ShelterType` と同値 / #162）。
 * facilityCategory は施設種別 code（運営側で拡張されうる開いた集合）。
 * capacity / externalUrl は任意のため未設定時は JSON から除外する。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ShelterResponse(
    val id: String,
    val name: String,
    val address: String,
    val coordinates: CoordinatesResponse,
    val type: String,
    val facilityCategory: String,
    val media: MediaResponse,
    val capacity: Int?,
    val accessible: Boolean,
    val externalUrl: String?,
) {
    data class CoordinatesResponse(
        val latitude: Double,
        val longitude: Double,
    )

    data class MediaResponse(
        val imageUrl: String,
    )

    companion object {
        fun from(view: ShelterView): ShelterResponse =
            ShelterResponse(
                id = view.id,
                name = view.name,
                address = view.address,
                coordinates = CoordinatesResponse(latitude = view.latitude, longitude = view.longitude),
                type = view.type,
                facilityCategory = view.facilityCategory,
                media = MediaResponse(imageUrl = view.imageUrl),
                capacity = view.capacity,
                accessible = view.accessible,
                externalUrl = view.externalUrl,
            )
    }
}

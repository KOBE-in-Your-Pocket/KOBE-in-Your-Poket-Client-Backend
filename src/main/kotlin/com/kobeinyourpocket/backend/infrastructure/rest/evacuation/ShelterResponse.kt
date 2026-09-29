package com.kobeinyourpocket.backend.infrastructure.rest.evacuation

import com.fasterxml.jackson.annotation.JsonInclude
import com.kobeinyourpocket.backend.application.evacuation.query.ShelterView
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.DisasterType

/**
 * `GET /api/v1/evacuation/shelters` のレスポンス（Client `EvacuationShelter` 形）。
 *
 * type は `emergency|designated|both`、siting は `indoor|outdoor` のリテラル。
 * [suitability] は災害種別 slug をキーにしたオブジェクトで、**常に全種別を含む**。
 * Client は絞り込みでこのキーを引くため、欠けると「対応していない」と「情報が無い」の
 * 区別が付かなくなる（#180）。
 *
 * phoneNumber / note は無い避難所が多いため、未設定時は JSON から除外する。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ShelterResponse(
    val id: String,
    val name: String,
    val address: String,
    val coordinates: CoordinatesResponse,
    val type: String,
    val siting: String,
    val suitability: Map<String, String>,
    val petAcceptance: String,
    val phoneNumber: String?,
    val note: String?,
) {
    data class CoordinatesResponse(
        val latitude: Double,
        val longitude: Double,
    )

    companion object {
        fun from(view: ShelterView): ShelterResponse =
            ShelterResponse(
                id = view.id,
                name = view.name,
                address = view.address,
                coordinates = CoordinatesResponse(latitude = view.latitude, longitude = view.longitude),
                type = view.type,
                siting = view.siting,
                suitability =
                    mapOf(
                        DisasterType.LANDSLIDE.wireValue to view.suitabilityLandslide,
                        DisasterType.FLOOD.wireValue to view.suitabilityFlood,
                        DisasterType.TSUNAMI.wireValue to view.suitabilityTsunami,
                        DisasterType.LARGE_FIRE.wireValue to view.suitabilityLargeFire,
                    ),
                petAcceptance = view.petAcceptance,
                phoneNumber = view.phoneNumber,
                note = view.note,
            )
    }
}

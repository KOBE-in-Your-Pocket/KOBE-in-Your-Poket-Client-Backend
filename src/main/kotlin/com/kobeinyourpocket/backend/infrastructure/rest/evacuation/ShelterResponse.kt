package com.kobeinyourpocket.backend.infrastructure.rest.evacuation

import com.fasterxml.jackson.annotation.JsonInclude
import com.kobeinyourpocket.backend.application.evacuation.query.ShelterView
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.DisasterType
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterSiting

/**
 * `GET /api/v1/evacuation/shelters` のレスポンス（Client `EvacuationShelter` 形）。
 *
 * type は `emergency|designated|both`、siting は `indoor|outdoor` のリテラル。
 * [suitability] は災害種別 slug をキーにしたオブジェクトで、**常に全種別を含む**。
 * Client は絞り込みでこのキーを引くため、欠けると「対応していない」と「情報が無い」の
 * 区別が付かなくなる（#180）。
 *
 * phoneNumber / note は無い避難所が多いため、未設定時は JSON から除外する。
 *
 * **[facilityCategory] / [media] / [accessible] は公開済み 1.0.0 のための互換用**
 * （Client #557）。1.0.0 はこの 3 つを必須として読み、レスポンスを検証せずそのまま
 * SQLite へ流すため、欠けると新規インストールの利用者に避難所が 1 件も出なくなる。
 * 新しい形に対応した Client が行き渡ったら 3 つとも消すこと。
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
    /**
     * 互換用（Client #557）。1.0.0 はこの値でアイコンとラベルを選ぶ。
     *
     * 元データは施設種別を持たないため [ShelterSiting] から当てる。屋内は
     * `government`（公共施設）で、どの避難所でも事実に反しない。`school` にすると
     * 大学・会館・体育館で嘘になる。屋外は `park`（公園）。
     */
    val facilityCategory: String,
    /** 互換用（Client #557）。画像は持たないため常に空文字。1.0.0 は空文字でプレースホルダを出す。 */
    val media: MediaResponse,
    /**
     * 互換用（Client #557）。元データはバリアフリー情報を持たない。
     *
     * 常に false。差し替え前のシード 11 件も全件 false だったので、1.0.0 の表示は変わらない。
     */
    val accessible: Boolean,
) {
    data class CoordinatesResponse(
        val latitude: Double,
        val longitude: Double,
    )

    /** 互換用（Client #557）。 */
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
                facilityCategory = compatFacilityCategory(view.siting),
                media = MediaResponse(imageUrl = ""),
                accessible = false,
            )

        /** 互換用（Client #557）。屋内は公共施設、屋外は公園に当てる。 */
        private fun compatFacilityCategory(siting: String): String =
            when (siting) {
                ShelterSiting.OUTDOOR.wireValue -> "park"
                else -> "government"
            }
    }
}

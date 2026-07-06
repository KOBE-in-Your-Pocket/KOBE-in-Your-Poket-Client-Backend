package com.kobeinyourpocket.backend.domain.evacuation.vo

/**
 * 緯度・経度の Value Object。
 *
 * Client `ShelterCoordinates` に対応する。`aggregate.EvacuationShelter` がコンポジションで保持する。
 * [Companion.of] が生成入口（範囲チェックは `init` で実施）。
 */
data class ShelterCoordinates(
    val latitude: Double,
    val longitude: Double,
) {
    init {
        require(latitude in LATITUDE_RANGE) {
            "latitude must be between -90 and 90, got $latitude"
        }
        require(longitude in LONGITUDE_RANGE) {
            "longitude must be between -180 and 180, got $longitude"
        }
    }

    companion object {
        private val LATITUDE_RANGE = -90.0..90.0
        private val LONGITUDE_RANGE = -180.0..180.0

        fun of(
            latitude: Double,
            longitude: Double,
        ): ShelterCoordinates = ShelterCoordinates(latitude, longitude)
    }
}

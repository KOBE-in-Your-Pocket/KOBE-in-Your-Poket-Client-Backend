package com.kobeinyourpocket.backend.domain.tourism.model

/**
 * 観光スポットの位置（緯度・経度）。
 *
 * Client `SpotCoordinates` に対応する。
 */
data class Coordinates(
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
        ): Coordinates = Coordinates(latitude, longitude)
    }
}

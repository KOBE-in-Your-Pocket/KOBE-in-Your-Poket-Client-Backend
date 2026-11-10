package com.kobeinyourpocket.backend.domain.weather

/**
 * [値オブジェクト] 天気を取得する地点の緯度・経度（#230）。
 *
 * 範囲チェックは `init` で行う。
 */
data class WeatherPoint(
    val latitude: Double,
    val longitude: Double,
) {
    init {
        require(latitude in LATITUDE_RANGE) { "latitude must be between -90 and 90, got $latitude" }
        require(longitude in LONGITUDE_RANGE) { "longitude must be between -180 and 180, got $longitude" }
    }

    private companion object {
        val LATITUDE_RANGE = -90.0..90.0
        val LONGITUDE_RANGE = -180.0..180.0
    }
}

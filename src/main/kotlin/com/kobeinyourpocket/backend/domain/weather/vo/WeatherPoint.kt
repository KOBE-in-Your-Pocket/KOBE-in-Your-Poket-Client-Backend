package com.kobeinyourpocket.backend.domain.weather.vo

/**
 * [値オブジェクト] 天気を取得する地点の緯度・経度（#230）。
 *
 * 範囲チェックは `init` で行う。
 * Yahoo! へ渡すときの「経度,緯度」の並びは infrastructure 層の都合のため、ここでは持たない。
 */
data class WeatherPoint(
    val latitude: Double,
    val longitude: Double,
) {
    init {
        require(latitude in LATITUDE_RANGE) { "latitude must be between -90 and 90, got $latitude" }
        require(longitude in LONGITUDE_RANGE) { "longitude must be between -180 and 180, got $longitude" }
    }

    companion object {
        private val LATITUDE_RANGE = -90.0..90.0
        private val LONGITUDE_RANGE = -180.0..180.0

        /**
         * 神戸の中心（神戸市役所・三宮付近）。天気を取得する地点（#231）。
         *
         * 今は神戸の中心 1 地点だけを取得する。エリアを増やすときは地点の一覧に置き換える。
         */
        val KOBE_CENTER = WeatherPoint(latitude = 34.690083, longitude = 135.195511)
    }
}

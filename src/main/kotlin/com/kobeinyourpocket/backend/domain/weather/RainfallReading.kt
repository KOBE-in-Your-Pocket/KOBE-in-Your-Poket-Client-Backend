package com.kobeinyourpocket.backend.domain.weather

import java.time.Instant

/** 降水強度の値が観測値か予測値か（#230）。 */
enum class RainfallReadingType {
    /** 気象レーダーで観測した、今の降水強度。 */
    OBSERVATION,

    /** 60 分先までの予測値。 */
    FORECAST,
}

/**
 * [値オブジェクト] ある時刻の降水強度（#230）。
 *
 * [mmPerHour] は気象レーダーの降水の強さを時間雨量（mm/h）に換算した値で、実際の雨量とは異なる。
 */
data class RainfallReading(
    val type: RainfallReadingType,
    val at: Instant,
    val mmPerHour: Double,
) {
    init {
        require(mmPerHour >= 0.0) { "mmPerHour must not be negative, got $mmPerHour" }
    }
}

/** 1 地点の天気。観測値と予測値を時刻順に持つ（#230）。 */
data class PointWeather(
    val point: WeatherPoint,
    val readings: List<RainfallReading>,
)

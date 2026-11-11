package com.kobeinyourpocket.backend.domain.weather.model

import com.kobeinyourpocket.backend.domain.weather.vo.RainfallReading
import com.kobeinyourpocket.backend.domain.weather.vo.WeatherPoint

/**
 * [モデル] 1 地点の天気（#230）。
 *
 * 地点 [point] と、その地点の降水強度 [readings]（今の観測値と 60 分先までの予測値）を時刻順に持つ。
 */
data class PointWeather(
    val point: WeatherPoint,
    val readings: List<RainfallReading>,
)

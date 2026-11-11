package com.kobeinyourpocket.backend.domain.weather.model

import java.time.Instant

/**
 * [モデル] ある時刻に取得した 1 地点の天気（#231）。
 *
 * [weather] は取得した内容、[fetchedAt] は backend が気象情報 API から取得した時刻。
 * 保存（[com.kobeinyourpocket.backend.domain.weather.repository.WeatherRepository]）の単位。
 */
data class WeatherSnapshot(
    val weather: PointWeather,
    val fetchedAt: Instant,
)

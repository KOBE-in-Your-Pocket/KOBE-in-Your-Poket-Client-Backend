package com.kobeinyourpocket.backend.application.weather.query

import java.time.Instant

/**
 * 地点の最新の天気の読みモデル（#233）。`weather_readings` の最新の取得分に対応する。
 *
 * Client が天気マーク（雨なし・まもなく雨・弱い雨・雨・強い雨）を判定しやすいよう、
 * 今の観測値 [observation] と 60 分先までの予測値 [forecasts]（時刻順）を分けて持つ。
 *
 * 地点（緯度・経度）は持たない。今は神戸の中心 1 地点だけで、Client は地点を使わないため。
 * エリアを増やすときに、地点の識別子とあわせて足す。
 */
data class LatestWeatherView(
    /** backend が気象情報 API から取得した時刻。古さの判断に使う。 */
    val fetchedAt: Instant,
    val observation: RainfallView,
    val forecasts: List<RainfallView>,
)

/** ある時刻の降水強度（mm/h）。 */
data class RainfallView(
    val at: Instant,
    val mmPerHour: Double,
)

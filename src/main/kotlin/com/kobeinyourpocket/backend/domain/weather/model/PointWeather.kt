package com.kobeinyourpocket.backend.domain.weather.model

import com.kobeinyourpocket.backend.domain.weather.vo.RainfallReading
import com.kobeinyourpocket.backend.domain.weather.vo.RainfallReadingType
import com.kobeinyourpocket.backend.domain.weather.vo.WeatherPoint

/**
 * [モデル] 1 地点の天気（#230）。
 *
 * 地点 [point] と、その地点の降水強度 [readings]（今の観測値と 60 分先までの予測値）を時刻順に持つ。
 *
 * 不変条件:
 * - [readings] は時刻の昇順で、同じ時刻の値は無い
 * - 観測値（[RainfallReadingType.OBSERVATION]）がちょうど 1 件ある。
 *   観測値の無い天気（空・予測値だけ）を作れないようにし、取得に失敗したときに前回のデータを空で上書きしないようにする
 */
data class PointWeather(
    val point: WeatherPoint,
    val readings: List<RainfallReading>,
) {
    init {
        require(readings.zipWithNext().all { (prev, next) -> prev.at < next.at }) {
            "readings must be in strictly ascending order of time"
        }
        val observations = readings.count { it.type == RainfallReadingType.OBSERVATION }
        require(observations == 1) { "readings must contain exactly 1 observation, got $observations" }
    }
}

package com.kobeinyourpocket.backend.application.weather.query

import com.kobeinyourpocket.backend.domain.weather.vo.WeatherPoint
import org.springframework.stereotype.Service

/**
 * 最新の天気を取得するユースケース（read / #233）。domain 集約を経由せず [WeatherQuery] port へ委譲する。
 *
 * 対象は取得バッチ（#232）と同じ神戸の中心 1 地点（[WeatherPoint.KOBE_CENTER]）。
 */
@Service
class GetLatestWeatherService(
    private val weatherQuery: WeatherQuery,
) {
    /** まだ 1 度も取得できていなければ null。 */
    fun getLatest(): LatestWeatherView? = weatherQuery.findLatest(WeatherPoint.KOBE_CENTER)
}

package com.kobeinyourpocket.backend.infrastructure.rest.weather

import com.kobeinyourpocket.backend.application.weather.query.LatestWeatherView
import com.kobeinyourpocket.backend.application.weather.query.RainfallView
import java.time.Instant

/**
 * `GET /api/v1/weather` のレスポンス（#233）。
 *
 * - [observation] は今の降水強度（観測値）、[forecasts] は 60 分先までの 10 分ごとの予測値（時刻順）
 * - 降水強度は mm/h。気象レーダーの値を時間雨量に換算したもので、実際の雨量とは異なる
 * - [fetchedAt] は backend が Yahoo! から取得した時刻（15 分ごと）。取得の失敗が続くと古いままになる
 * - 対象は神戸の中心 1 地点。地点（緯度・経度）は Client が使わないため返さない
 */
data class WeatherResponse(
    val fetchedAt: Instant,
    val observation: RainfallResponse,
    val forecasts: List<RainfallResponse>,
) {
    companion object {
        fun from(view: LatestWeatherView): WeatherResponse =
            WeatherResponse(
                fetchedAt = view.fetchedAt,
                observation = RainfallResponse.from(view.observation),
                forecasts = view.forecasts.map(RainfallResponse::from),
            )
    }
}

/** ある時刻の降水強度。 */
data class RainfallResponse(
    val at: Instant,
    val mmPerHour: Double,
) {
    companion object {
        fun from(view: RainfallView): RainfallResponse = RainfallResponse(at = view.at, mmPerHour = view.mmPerHour)
    }
}

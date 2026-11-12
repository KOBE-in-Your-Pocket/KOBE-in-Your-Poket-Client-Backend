package com.kobeinyourpocket.backend.application.weather.query

import com.kobeinyourpocket.backend.domain.weather.vo.WeatherPoint

/** read 専用 port。application が定義し infrastructure.query が実装する（#233）。 */
interface WeatherQuery {
    /** 地点の最新の天気を返す。まだ 1 度も保存していなければ null。 */
    fun findLatest(point: WeatherPoint): LatestWeatherView?
}

package com.kobeinyourpocket.backend.infrastructure.rest.weather

import com.kobeinyourpocket.backend.application.weather.query.GetLatestWeatherService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 天気の REST inbound adapter（#233）。application 経由のみ（persistence 直叩き禁止 / §2）。
 *
 * 認証不要（閲覧系は SecurityConfig で permitAll）。Client のマップ上部の天気マークに使う。
 * 言語に依存する値を返さないため、`?lang=` は受け取らない。
 */
@RestController
@RequestMapping("/api/v1/weather")
class WeatherController(
    private val getLatestWeatherService: GetLatestWeatherService,
) {
    /**
     * 神戸の中心の最新の天気を返す。
     *
     * まだ 1 度も取得できていない（取得バッチが未実行・Client ID が未設定など）ときは 204 No Content。
     * エラーではないため 404 にはしない。Client は天気マークを出さない。
     */
    @GetMapping
    fun getLatestWeather(): ResponseEntity<WeatherResponse> =
        getLatestWeatherService
            .getLatest()
            ?.let { ResponseEntity.ok(WeatherResponse.from(it)) }
            ?: ResponseEntity.noContent().build()
}

package com.kobeinyourpocket.backend.infrastructure.weather

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Yahoo! 気象情報API の呼び出し用（#229）。
 *
 * [clientId] は Yahoo! デベロッパーネットワークで発行した Client ID（アプリケーション ID）。
 * 空なら [isConfigured] が false になり、天気の取得バッチを動かさない（起動は妨げない）。
 * 詳細: docs/weather.md
 */
@ConfigurationProperties(prefix = "yahoo.weather")
data class YahooWeatherProperties(
    val clientId: String = "",
) {
    val isConfigured: Boolean
        get() = clientId.isNotBlank()

    // Client ID をログへ出さない。
    override fun toString(): String = "YahooWeatherProperties(configured=$isConfigured)"
}

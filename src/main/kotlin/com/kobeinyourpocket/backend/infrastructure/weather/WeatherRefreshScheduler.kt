package com.kobeinyourpocket.backend.infrastructure.weather

import com.kobeinyourpocket.backend.application.weather.command.RefreshWeatherService
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * 天気を定期的に取得して保存するバッチ（#232）。
 *
 * 既定では起動 30 秒後に 1 回目を実行し、以降 15 分ごとに [RefreshWeatherService] を呼ぶ。
 * 間隔は `weather.refresh.interval`、1 回目までの待ち時間は `weather.refresh.initial-delay`（ISO-8601 の期間）で変えられる。
 * `weather.refresh.enabled=false` で止められる（テストでは止める）。
 *
 * 今は EC2 1 台で動かすため、二重に実行されることはない。台数を増やすときは、
 * 1 台だけで動くようにする仕組み（ロックなど）が必要。
 */
@Component
@ConditionalOnProperty(prefix = "weather.refresh", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class WeatherRefreshScheduler(
    private val refreshWeatherService: RefreshWeatherService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(
        initialDelayString = "\${weather.refresh.initial-delay:PT30S}",
        fixedRateString = "\${weather.refresh.interval:PT15M}",
    )
    fun refresh() {
        try {
            refreshWeatherService.execute()
        } catch (ex: Exception) {
            // DB の失敗など。次の周期は実行したいので、ここで止めてログに残す。
            logger.error("天気の取得・保存に失敗した", ex)
        }
    }
}

/**
 * 定期実行（`@Scheduled`）を有効にする（#232）。このリポジトリで初めての定期実行。
 *
 * バッチを止めているとき（`weather.refresh.enabled=false`）は、スケジューラ自体も作らない。
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "weather.refresh", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class WeatherSchedulingConfig

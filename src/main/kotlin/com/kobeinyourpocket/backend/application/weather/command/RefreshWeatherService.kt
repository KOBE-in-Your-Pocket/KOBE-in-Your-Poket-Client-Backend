package com.kobeinyourpocket.backend.application.weather.command

import com.kobeinyourpocket.backend.application.weather.WeatherFetchFailureReason
import com.kobeinyourpocket.backend.application.weather.WeatherFetchResult
import com.kobeinyourpocket.backend.application.weather.WeatherGateway
import com.kobeinyourpocket.backend.domain.weather.model.WeatherSnapshot
import com.kobeinyourpocket.backend.domain.weather.repository.WeatherRepository
import com.kobeinyourpocket.backend.domain.weather.vo.WeatherPoint
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Clock

/**
 * 天気を取得して保存し直すユースケース（#232）。15 分ごとのバッチ（infrastructure の scheduler）から呼ぶ。
 *
 * - 取得できたら、地点ごとに保存している天気を置き換える
 * - 取得できなかったら何もしない（前回のデータを残し、次の周期で取り直す）
 * - Client ID が未設定なら、取得しない
 *
 * 気象情報 API の失敗は [WeatherGateway] が [WeatherFetchResult.Failure] で返すため、ここでは例外にならない。
 * 保存（DB）の失敗は例外のまま呼び出し元へ返す。
 */
@Service
class RefreshWeatherService(
    private val weatherGateway: WeatherGateway,
    private val weatherRepository: WeatherRepository,
    /** テストから固定時刻を渡すための時計。Bean が無い場合はシステム時計を使う。 */
    private val clock: Clock = Clock.systemUTC(),
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun execute(): RefreshWeatherResult {
        if (!weatherGateway.isConfigured) {
            logger.debug("天気の取得をスキップした（Client ID が未設定）")
            return RefreshWeatherResult.SKIPPED_NOT_CONFIGURED
        }

        return when (val result = weatherGateway.fetch(TARGET_POINTS)) {
            is WeatherFetchResult.Success -> {
                val fetchedAt = clock.instant()
                result.weathers.forEach { weatherRepository.replace(WeatherSnapshot(it, fetchedAt)) }
                logger.info("天気を保存した（{} 地点）", result.weathers.size)
                RefreshWeatherResult.REFRESHED
            }
            is WeatherFetchResult.Failure -> {
                // 未設定は上で弾いているため、ここに来るのは API・通信・応答の失敗。前回のデータを残す。
                if (result.reason == WeatherFetchFailureReason.NOT_CONFIGURED) {
                    return RefreshWeatherResult.SKIPPED_NOT_CONFIGURED
                }
                logger.warn("天気を取得できなかったため、前回のデータを残す（reason={}, {}）", result.reason, result.message)
                RefreshWeatherResult.FAILED
            }
        }
    }

    companion object {
        /** 天気を取得する地点。今は神戸の中心 1 地点（#231）。 */
        val TARGET_POINTS: List<WeatherPoint> = listOf(WeatherPoint.KOBE_CENTER)
    }
}

/** [RefreshWeatherService.execute] の結果。 */
enum class RefreshWeatherResult {
    /** 取得して保存し直した。 */
    REFRESHED,

    /** Client ID が未設定のため、取得しなかった。 */
    SKIPPED_NOT_CONFIGURED,

    /** 取得できなかったため、前回のデータを残した。 */
    FAILED,
}

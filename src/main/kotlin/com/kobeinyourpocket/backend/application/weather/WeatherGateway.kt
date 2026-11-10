package com.kobeinyourpocket.backend.application.weather

import com.kobeinyourpocket.backend.domain.weather.PointWeather
import com.kobeinyourpocket.backend.domain.weather.WeatherPoint

/**
 * 外部の気象情報 API（Yahoo! 気象情報API）への outbound port（#230）。
 *
 * 失敗は例外ではなく [WeatherFetchResult.Failure] で返す。天気の取得バッチ（#232）は
 * 失敗しても前回のデータを残して次の周期を待つだけなので、呼び出し側で例外を捕まえさせない。
 */
interface WeatherGateway {
    /** Client ID が設定されていて、API を呼べるか。 */
    val isConfigured: Boolean

    /**
     * 指定した地点の、今の降水強度（観測値）と 60 分先までの予測値を取得する。
     *
     * @param points 1〜[MAX_POINTS] 地点
     */
    fun fetch(points: List<WeatherPoint>): WeatherFetchResult

    companion object {
        /** 1 回の呼び出しで取得できる地点の上限（Yahoo! 気象情報API の仕様）。 */
        const val MAX_POINTS = 10
    }
}

/** [WeatherGateway.fetch] の結果。 */
sealed interface WeatherFetchResult {
    /** 取得できた。[weathers] は渡した地点と同じ順番。 */
    data class Success(
        val weathers: List<PointWeather>,
    ) : WeatherFetchResult

    /** 取得できなかった。[message] はログ用で、Client ID を含めない。 */
    data class Failure(
        val reason: WeatherFetchFailureReason,
        val message: String,
    ) : WeatherFetchResult
}

/** 取得できなかった理由。 */
enum class WeatherFetchFailureReason {
    /** Client ID が未設定のため呼ばなかった。 */
    NOT_CONFIGURED,

    /** API がエラーを返した（400 / 503 など）。 */
    API_ERROR,

    /** タイムアウト・接続失敗など、応答を受け取れなかった。 */
    NETWORK_ERROR,

    /** 応答の形が想定と違った。 */
    INVALID_RESPONSE,
}

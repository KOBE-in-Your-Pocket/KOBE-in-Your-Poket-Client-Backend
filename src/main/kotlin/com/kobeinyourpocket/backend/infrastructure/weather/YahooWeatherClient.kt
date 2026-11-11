package com.kobeinyourpocket.backend.infrastructure.weather

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.kobeinyourpocket.backend.application.weather.WeatherFetchFailureReason
import com.kobeinyourpocket.backend.application.weather.WeatherFetchResult
import com.kobeinyourpocket.backend.application.weather.WeatherGateway
import com.kobeinyourpocket.backend.domain.weather.model.PointWeather
import com.kobeinyourpocket.backend.domain.weather.vo.RainfallReading
import com.kobeinyourpocket.backend.domain.weather.vo.RainfallReadingType
import com.kobeinyourpocket.backend.domain.weather.vo.WeatherPoint
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import kotlin.math.abs

/**
 * Yahoo! 気象情報API のクライアント（#230）。
 *
 * `GET https://map.yahooapis.jp/weather/V1/place` を `output=json` で呼び、
 * 地点ごとの降水強度（今の観測値と 60 分先までの 10 分ごとの予測値）を返す。
 * 仕様: https://developer.yahoo.co.jp/webapi/map/openlocalplatform/v1/weather.html
 *
 * - 失敗（未設定・API エラー・タイムアウト・想定外の応答）は例外にせず [WeatherFetchResult.Failure] で返す
 * - Client ID（`appid`）はログ・失敗メッセージに出さない
 */
@Component
class YahooWeatherClient internal constructor(
    private val props: YahooWeatherProperties,
    private val restClient: RestClient,
) : WeatherGateway {
    @Autowired
    constructor(props: YahooWeatherProperties) : this(props, defaultRestClient())

    override val isConfigured: Boolean
        get() = props.isConfigured

    override fun fetch(points: List<WeatherPoint>): WeatherFetchResult {
        require(points.isNotEmpty()) { "points must not be empty" }
        require(points.size <= WeatherGateway.MAX_POINTS) {
            "points must be at most ${WeatherGateway.MAX_POINTS}, got ${points.size}"
        }
        if (!isConfigured) {
            return failure(WeatherFetchFailureReason.NOT_CONFIGURED, "Yahoo! の Client ID が未設定")
        }

        val response =
            try {
                restClient
                    .get()
                    .uri { builder ->
                        builder
                            .path(PLACE_PATH)
                            .queryParam("coordinates", "{coordinates}")
                            .queryParam("output", "json")
                            .queryParam("interval", INTERVAL_MINUTES)
                            .queryParam("appid", "{appid}")
                            .build(points.joinToString(" ") { it.toQueryValue() }, props.clientId)
                    }.retrieve()
                    .body(YahooWeatherResponse::class.java)
            } catch (ex: RestClientResponseException) {
                // エラー本文は {"Error":{"Code":"400","Message":"Bad Request."}} 程度で appid を含まない。
                return failure(
                    WeatherFetchFailureReason.API_ERROR,
                    "status=${ex.statusCode.value()}, body=${ex.responseBodyAsString.take(200)}",
                )
            } catch (ex: RestClientException) {
                // タイムアウト・接続失敗（ResourceAccessException）や、本文を読めなかった場合。
                // 例外メッセージには URL（appid を含む）が入ることがあるため、型名だけを残す。
                val reason =
                    if (ex is ResourceAccessException) {
                        WeatherFetchFailureReason.NETWORK_ERROR
                    } else {
                        WeatherFetchFailureReason.INVALID_RESPONSE
                    }
                return failure(reason, ex.javaClass.simpleName)
            }

        return toResult(points, response)
    }

    private fun toResult(
        points: List<WeatherPoint>,
        response: YahooWeatherResponse?,
    ): WeatherFetchResult {
        if (response == null) return failure(WeatherFetchFailureReason.INVALID_RESPONSE, "empty body")
        response.error?.let { return failure(WeatherFetchFailureReason.API_ERROR, "code=${it.code}, message=${it.message}") }

        val features = response.features.orEmpty()
        if (features.size != points.size) {
            return failure(
                WeatherFetchFailureReason.INVALID_RESPONSE,
                "feature count ${features.size} does not match point count ${points.size}",
            )
        }

        val weathers =
            try {
                matchFeatures(points, features).map { (point, feature) -> PointWeather(point, feature.toReadings()) }
            } catch (ex: InvalidYahooWeatherResponse) {
                return failure(WeatherFetchFailureReason.INVALID_RESPONSE, ex.message.orEmpty())
            } catch (ex: IllegalArgumentException) {
                // ドメインの不変条件（観測値がちょうど 1 件・時刻の重複なし など）を満たさない応答。
                // 例外ではなく Failure で返し、#232 のバッチが前回のデータを空で上書きしないようにする。
                return failure(WeatherFetchFailureReason.INVALID_RESPONSE, ex.message.orEmpty())
            }
        return WeatherFetchResult.Success(weathers)
    }

    /**
     * 地点ごとに、その地点の Feature を対応づける。
     *
     * 並び順だけで対応づけると、順番がずれても件数は合うため、エラーにならずに別の地点のデータが入ってしまう。
     * Yahoo! のドキュメントは「渡した順に返す」と保証していないので、Feature の `Geometry.Coordinates` を
     * 数値に直し、送った地点と [COORDINATE_TOLERANCE] の範囲で一致するもので対応づける。
     * 文字列で比べないのは、Yahoo! の桁数・丸め方が変わっても正しく対応づけるため。
     *
     * 一度対応づけた Feature は使い回さない（同じ地点を 2 回渡されても、それぞれ別の Feature に対応づける）。
     * 対応する Feature が無い地点があれば [InvalidYahooWeatherResponse]。件数は事前に一致を確かめているので、
     * すべての地点が対応づけば、余る Feature は無い。
     */
    private fun matchFeatures(
        points: List<WeatherPoint>,
        features: List<Feature>,
    ): List<Pair<WeatherPoint, Feature>> {
        val remaining = features.map { it to it.coordinates() }.toMutableList()
        return points.map { point ->
            val index = remaining.indexOfFirst { (_, coordinates) -> coordinates.matches(point) }
            if (index < 0) {
                throw InvalidYahooWeatherResponse("no Feature for point ${point.toQueryValue()}")
            }
            point to remaining.removeAt(index).first
        }
    }

    private fun failure(
        reason: WeatherFetchFailureReason,
        message: String,
    ) = WeatherFetchResult.Failure(reason, "Yahoo! 気象情報API: $message")

    // --- Yahoo! のレスポンス（output=json） ---

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class YahooWeatherResponse(
        @JsonProperty("Feature") val features: List<Feature>? = null,
        @JsonProperty("Error") val error: Error? = null,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Error(
        @JsonProperty("Code") val code: String? = null,
        @JsonProperty("Message") val message: String? = null,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Feature(
        @JsonProperty("Geometry") val geometry: Geometry? = null,
        @JsonProperty("Property") val property: Property? = null,
    ) {
        /** `Geometry.Coordinates`（「経度,緯度」の文字列）を数値に直す。 */
        fun coordinates(): FeatureCoordinates {
            val raw =
                geometry?.coordinates
                    ?: throw InvalidYahooWeatherResponse("Feature has no Geometry.Coordinates")
            val parts = raw.split(",").map { it.trim().toDoubleOrNull() }
            val longitude = parts.getOrNull(0)
            val latitude = parts.getOrNull(1)
            if (parts.size != 2 || longitude == null || latitude == null) {
                throw InvalidYahooWeatherResponse("invalid Geometry.Coordinates: $raw")
            }
            return FeatureCoordinates(longitude = longitude, latitude = latitude)
        }

        fun toReadings(): List<RainfallReading> {
            val weathers =
                property?.weatherList?.weathers
                    ?: throw InvalidYahooWeatherResponse("Feature has no WeatherList")
            return weathers.map { it.toReading() }.sortedBy { it.at }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Geometry(
        @JsonProperty("Coordinates") val coordinates: String? = null,
    )

    /** Feature の地点（応答の `Geometry.Coordinates` を数値に直したもの）。 */
    data class FeatureCoordinates(
        val longitude: Double,
        val latitude: Double,
    ) {
        fun matches(point: WeatherPoint): Boolean =
            abs(longitude - point.longitude) <= COORDINATE_TOLERANCE &&
                abs(latitude - point.latitude) <= COORDINATE_TOLERANCE
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Property(
        @JsonProperty("WeatherList") val weatherList: WeatherList? = null,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class WeatherList(
        @JsonProperty("Weather") val weathers: List<Weather>? = null,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Weather(
        @JsonProperty("Type") val type: String? = null,
        @JsonProperty("Date") val date: String? = null,
        @JsonProperty("Rainfall") val rainfall: Double? = null,
    ) {
        fun toReading(): RainfallReading {
            val readingType =
                when (type) {
                    "observation" -> RainfallReadingType.OBSERVATION
                    "forecast" -> RainfallReadingType.FORECAST
                    else -> throw InvalidYahooWeatherResponse("unknown Type: $type")
                }
            val rawDate = date ?: throw InvalidYahooWeatherResponse("missing Date")
            val at =
                try {
                    LocalDateTime.parse(rawDate, DATE_FORMAT).atZone(JST).toInstant()
                } catch (ex: DateTimeParseException) {
                    throw InvalidYahooWeatherResponse("invalid Date: $rawDate")
                }
            val mmPerHour = rainfall ?: throw InvalidYahooWeatherResponse("missing Rainfall")
            // RainfallReading の不変条件（有限・0 以上）を先に確かめ、例外ではなく Failure にする。
            if (!mmPerHour.isFinite() || mmPerHour < 0.0) throw InvalidYahooWeatherResponse("invalid Rainfall: $mmPerHour")
            return RainfallReading(readingType, at, mmPerHour)
        }
    }

    private class InvalidYahooWeatherResponse(
        message: String,
    ) : RuntimeException(message)

    companion object {
        private const val BASE_URL = "https://map.yahooapis.jp"
        private const val PLACE_PATH = "/weather/V1/place"
        private const val INTERVAL_MINUTES = 10

        /**
         * 応答の座標と送った地点を同じとみなす誤差（度）。送るときは小数 6 桁に丸めるため、
         * その丸め（最大 0.0000005）より少し大きくする。地点どうしは十分離れている前提（約 0.1 m）。
         */
        private const val COORDINATE_TOLERANCE = 0.000001

        /** `Date` は日本時間の `YYYYMMDDHHMI`。 */
        private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm")
        private val JST: ZoneId = ZoneId.of("Asia/Tokyo")

        /** `coordinates` は「経度,緯度」の順（Yahoo! の仕様）。 */
        private fun WeatherPoint.toQueryValue(): String = String.format(Locale.ROOT, "%.6f,%.6f", longitude, latitude)

        private fun defaultRestClient(): RestClient =
            RestClient
                .builder()
                .baseUrl(BASE_URL)
                .requestFactory(
                    // 15 分ごとのバッチから呼ぶ。止まったときに次の周期へ響かないよう短めにする。
                    SimpleClientHttpRequestFactory().apply {
                        setConnectTimeout(Duration.ofSeconds(3))
                        setReadTimeout(Duration.ofSeconds(10))
                    },
                ).build()
    }
}

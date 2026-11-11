package com.kobeinyourpocket.backend.infrastructure.weather

import com.kobeinyourpocket.backend.application.weather.WeatherFetchFailureReason
import com.kobeinyourpocket.backend.application.weather.WeatherFetchResult
import com.kobeinyourpocket.backend.domain.weather.vo.RainfallReadingType
import com.kobeinyourpocket.backend.domain.weather.vo.WeatherPoint
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.RequestMatcher
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest
import org.springframework.test.web.client.response.MockRestResponseCreators.withException
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import org.springframework.web.util.UriComponentsBuilder
import java.net.SocketTimeoutException
import java.net.URLDecoder
import java.time.Instant
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs

/** Yahoo! 気象情報API の呼び出し（#230）。Yahoo! へは MockRestServiceServer で応答する。 */
class YahooWeatherClientTest {
    private val props = YahooWeatherProperties(clientId = "test-client-id")
    private val sannomiya = WeatherPoint(latitude = 34.6913, longitude = 135.1955)
    private val harborland = WeatherPoint(latitude = 34.68, longitude = 135.18)

    private lateinit var builder: RestClient.Builder
    private lateinit var server: MockRestServiceServer
    private lateinit var client: YahooWeatherClient

    @BeforeTest
    fun setUp() {
        builder = RestClient.builder().baseUrl("https://map.yahooapis.jp")
        server = MockRestServiceServer.bindTo(builder).build()
        client = YahooWeatherClient(props, builder.build())
    }

    /** クエリをデコードして比べる（RestClient は `,` と空白をエンコードして送る）。 */
    private fun query(
        name: String,
        expected: String,
    ) = RequestMatcher { request ->
        val raw =
            UriComponentsBuilder
                .fromUri(request.uri)
                .build()
                .queryParams
                .getFirst(name)
        assertEquals(expected, raw?.let { URLDecoder.decode(it, Charsets.UTF_8) }, "query $name")
    }

    private fun path(expected: String) = RequestMatcher { request -> assertEquals(expected, request.uri.path) }

    @Test
    fun `地点ごとに観測値と予測値を返す`() {
        server
            .expect(method(HttpMethod.GET))
            .andExpect(path("/weather/V1/place"))
            .andExpect(query("coordinates", "135.195500,34.691300 135.180000,34.680000"))
            .andExpect(query("output", "json"))
            .andExpect(query("interval", "10"))
            .andExpect(query("appid", "test-client-id"))
            .andRespond(withSuccess(TWO_POINTS_JSON, MediaType.APPLICATION_JSON))

        val result = client.fetch(listOf(sannomiya, harborland))

        server.verify()
        val success = assertIs<WeatherFetchResult.Success>(result)
        assertEquals(listOf(sannomiya, harborland), success.weathers.map { it.point })

        val readings = success.weathers[0].readings
        assertEquals(3, readings.size)
        assertEquals(RainfallReadingType.OBSERVATION, readings[0].type)
        // Date は日本時間（2026-11-10 13:45 JST = 04:45 UTC）
        assertEquals(Instant.parse("2026-11-10T04:45:00Z"), readings[0].at)
        assertEquals(1.65, readings[0].mmPerHour)
        assertEquals(listOf(RainfallReadingType.FORECAST, RainfallReadingType.FORECAST), readings.drop(1).map { it.type })
        assertEquals(0.85, readings[2].mmPerHour)

        assertEquals(0.0, success.weathers[1].readings[0].mmPerHour)
    }

    @Test
    fun `予測値が時刻順でなくても時刻順に並べる`() {
        server.expect(method(HttpMethod.GET)).andRespond(
            withSuccess(
                featureJson(
                    """{"Type":"forecast","Date":"202611101355","Rainfall":0.5},
                       {"Type":"observation","Date":"202611101345","Rainfall":0.0}""",
                ),
                MediaType.APPLICATION_JSON,
            ),
        )

        val success = assertIs<WeatherFetchResult.Success>(client.fetch(listOf(sannomiya)))

        assertEquals(
            listOf(RainfallReadingType.OBSERVATION, RainfallReadingType.FORECAST),
            success.weathers[0].readings.map { it.type },
        )
    }

    @Test
    fun `Client ID が未設定なら呼ばずに NOT_CONFIGURED を返す`() {
        val unconfigured = YahooWeatherClient(YahooWeatherProperties(), builder.build())

        val result = unconfigured.fetch(listOf(sannomiya))

        server.verify() // リクエストが 1 件も飛んでいない
        assertFalse(unconfigured.isConfigured)
        assertEquals(WeatherFetchFailureReason.NOT_CONFIGURED, assertIs<WeatherFetchResult.Failure>(result).reason)
    }

    @Test
    fun `400 は API_ERROR として返す`() {
        server.expect(method(HttpMethod.GET)).andRespond(
            withBadRequest().body("""{"Error":{"Code":"400","Message":"Bad Request."}}""").contentType(MediaType.APPLICATION_JSON),
        )

        val failure = assertIs<WeatherFetchResult.Failure>(client.fetch(listOf(sannomiya)))

        assertEquals(WeatherFetchFailureReason.API_ERROR, failure.reason)
        assertFalse(failure.message.contains("test-client-id"))
    }

    @Test
    fun `503 は API_ERROR として返す`() {
        server.expect(method(HttpMethod.GET)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE))

        val failure = assertIs<WeatherFetchResult.Failure>(client.fetch(listOf(sannomiya)))

        assertEquals(WeatherFetchFailureReason.API_ERROR, failure.reason)
    }

    @Test
    fun `200 でも Error が返ったら API_ERROR として返す`() {
        server.expect(method(HttpMethod.GET)).andRespond(
            withSuccess("""{"Error":{"Code":"400","Message":"Bad Request."}}""", MediaType.APPLICATION_JSON),
        )

        val failure = assertIs<WeatherFetchResult.Failure>(client.fetch(listOf(sannomiya)))

        assertEquals(WeatherFetchFailureReason.API_ERROR, failure.reason)
    }

    @Test
    fun `タイムアウトは NETWORK_ERROR として返し、メッセージに Client ID を含めない`() {
        server.expect(method(HttpMethod.GET)).andRespond(withException(SocketTimeoutException("Read timed out")))

        val failure = assertIs<WeatherFetchResult.Failure>(client.fetch(listOf(sannomiya)))

        assertEquals(WeatherFetchFailureReason.NETWORK_ERROR, failure.reason)
        assertFalse(failure.message.contains("test-client-id"))
    }

    @Test
    fun `地点の数と結果の数が合わなければ INVALID_RESPONSE を返す`() {
        server
            .expect(method(HttpMethod.GET))
            .andRespond(withSuccess(featureJson(OBSERVATION_ZERO), MediaType.APPLICATION_JSON))

        val failure = assertIs<WeatherFetchResult.Failure>(client.fetch(listOf(sannomiya, harborland)))

        assertEquals(WeatherFetchFailureReason.INVALID_RESPONSE, failure.reason)
    }

    @Test
    fun `知らない Type や不正な Date は INVALID_RESPONSE を返す`() {
        listOf(
            """{"Type":"unknown","Date":"202611101345","Rainfall":0.0}""",
            """{"Type":"observation","Date":"2026-11-10","Rainfall":0.0}""",
            """{"Type":"observation","Rainfall":0.0}""",
            """{"Type":"observation","Date":"202611101345"}""",
            """{"Type":"observation","Date":"202611101345","Rainfall":-1.0}""",
            """{"Type":"observation","Date":"202611101345","Rainfall":"NaN"}""",
            // double の範囲を超える値は Infinity として読まれる
            """{"Type":"observation","Date":"202611101345","Rainfall":1e400}""",
        ).forEach { weather ->
            setUp()
            server.expect(method(HttpMethod.GET)).andRespond(withSuccess(featureJson(weather), MediaType.APPLICATION_JSON))

            val failure = assertIs<WeatherFetchResult.Failure>(client.fetch(listOf(sannomiya)), weather)

            assertEquals(WeatherFetchFailureReason.INVALID_RESPONSE, failure.reason, weather)
        }
    }

    @Test
    fun `観測値が無い応答（空・予測値だけ）は INVALID_RESPONSE を返す`() {
        listOf(
            "",
            """{"Type":"forecast","Date":"202611101355","Rainfall":0.0},
               {"Type":"forecast","Date":"202611101405","Rainfall":0.0}""",
        ).forEach { weathers ->
            setUp()
            server.expect(method(HttpMethod.GET)).andRespond(withSuccess(featureJson(weathers), MediaType.APPLICATION_JSON))

            val failure = assertIs<WeatherFetchResult.Failure>(client.fetch(listOf(sannomiya)), weathers)

            assertEquals(WeatherFetchFailureReason.INVALID_RESPONSE, failure.reason, weathers)
        }
    }

    @Test
    fun `観測値が 2 件ある応答や、同じ時刻の値がある応答は INVALID_RESPONSE を返す`() {
        listOf(
            """{"Type":"observation","Date":"202611101345","Rainfall":0.0},
               {"Type":"observation","Date":"202611101355","Rainfall":0.0}""",
            """{"Type":"observation","Date":"202611101345","Rainfall":0.0},
               {"Type":"forecast","Date":"202611101345","Rainfall":0.0}""",
        ).forEach { weathers ->
            setUp()
            server.expect(method(HttpMethod.GET)).andRespond(withSuccess(featureJson(weathers), MediaType.APPLICATION_JSON))

            val failure = assertIs<WeatherFetchResult.Failure>(client.fetch(listOf(sannomiya)), weathers)

            assertEquals(WeatherFetchFailureReason.INVALID_RESPONSE, failure.reason, weathers)
        }
    }

    @Test
    fun `複数地点のうち 1 地点でも観測値が無ければ、全体を INVALID_RESPONSE にする`() {
        server.expect(method(HttpMethod.GET)).andRespond(
            withSuccess(
                featureJson(OBSERVATION_ZERO, """{"Type":"forecast","Date":"202611101355","Rainfall":0.0}"""),
                MediaType.APPLICATION_JSON,
            ),
        )

        val failure = assertIs<WeatherFetchResult.Failure>(client.fetch(listOf(sannomiya, harborland)))

        assertEquals(WeatherFetchFailureReason.INVALID_RESPONSE, failure.reason)
    }

    @Test
    fun `結果が渡した順に返らなくても、座標で正しい地点に対応づける`() {
        // ハーバーランド → 三宮 の順で返す（渡したのは 三宮 → ハーバーランド）
        server.expect(method(HttpMethod.GET)).andRespond(
            withSuccess(
                featureJsonWithCoordinates(
                    listOf(
                        "135.180000,34.680000" to """{"Type":"observation","Date":"202611101345","Rainfall":5.0}""",
                        "135.195500,34.691300" to """{"Type":"observation","Date":"202611101345","Rainfall":1.0}""",
                    ),
                ),
                MediaType.APPLICATION_JSON,
            ),
        )

        val success = assertIs<WeatherFetchResult.Success>(client.fetch(listOf(sannomiya, harborland)))

        // 結果は渡した地点の順。三宮には三宮の値、ハーバーランドにはハーバーランドの値が入る
        assertEquals(listOf(sannomiya, harborland), success.weathers.map { it.point })
        assertEquals(1.0, success.weathers[0].readings[0].mmPerHour)
        assertEquals(5.0, success.weathers[1].readings[0].mmPerHour)
    }

    @Test
    fun `座標の桁数が送ったときと違っても、数値が同じなら対応づける`() {
        server.expect(method(HttpMethod.GET)).andRespond(
            withSuccess(featureJsonWithCoordinates(listOf("135.1955,34.6913" to OBSERVATION_ZERO)), MediaType.APPLICATION_JSON),
        )

        assertIs<WeatherFetchResult.Success>(client.fetch(listOf(sannomiya)))
    }

    @Test
    fun `どの地点にも一致しない座標の結果は INVALID_RESPONSE を返す`() {
        server.expect(method(HttpMethod.GET)).andRespond(
            withSuccess(
                featureJsonWithCoordinates(
                    listOf(
                        "135.195500,34.691300" to OBSERVATION_ZERO,
                        // ハーバーランドではなく別の地点
                        "135.000000,34.000000" to OBSERVATION_ZERO,
                    ),
                ),
                MediaType.APPLICATION_JSON,
            ),
        )

        val failure = assertIs<WeatherFetchResult.Failure>(client.fetch(listOf(sannomiya, harborland)))

        assertEquals(WeatherFetchFailureReason.INVALID_RESPONSE, failure.reason)
    }

    @Test
    fun `同じ地点の結果が 2 つあり、ほかの地点の結果が無ければ INVALID_RESPONSE を返す`() {
        server.expect(method(HttpMethod.GET)).andRespond(
            withSuccess(
                featureJsonWithCoordinates(
                    listOf("135.195500,34.691300" to OBSERVATION_ZERO, "135.195500,34.691300" to OBSERVATION_ZERO),
                ),
                MediaType.APPLICATION_JSON,
            ),
        )

        val failure = assertIs<WeatherFetchResult.Failure>(client.fetch(listOf(sannomiya, harborland)))

        assertEquals(WeatherFetchFailureReason.INVALID_RESPONSE, failure.reason)
    }

    @Test
    fun `同じ地点を 2 回渡したときは、それぞれに結果を対応づける`() {
        server.expect(method(HttpMethod.GET)).andRespond(
            withSuccess(
                featureJsonWithCoordinates(
                    listOf("135.195500,34.691300" to OBSERVATION_ZERO, "135.195500,34.691300" to OBSERVATION_ZERO),
                ),
                MediaType.APPLICATION_JSON,
            ),
        )

        val success = assertIs<WeatherFetchResult.Success>(client.fetch(listOf(sannomiya, sannomiya)))

        assertEquals(2, success.weathers.size)
    }

    @Test
    fun `座標が無い・読めない結果は INVALID_RESPONSE を返す`() {
        listOf<String?>(null, "abc", "135.195500", "135.195500,34.691300,0").forEach { coordinates ->
            setUp()
            server.expect(method(HttpMethod.GET)).andRespond(
                withSuccess(featureJsonWithCoordinates(listOf(coordinates to OBSERVATION_ZERO)), MediaType.APPLICATION_JSON),
            )

            val failure = assertIs<WeatherFetchResult.Failure>(client.fetch(listOf(sannomiya)), "$coordinates")

            assertEquals(WeatherFetchFailureReason.INVALID_RESPONSE, failure.reason, "$coordinates")
        }
    }

    @Test
    fun `JSON として読めない応答は INVALID_RESPONSE を返す`() {
        server.expect(method(HttpMethod.GET)).andRespond(withSuccess("not json", MediaType.APPLICATION_JSON))

        val failure = assertIs<WeatherFetchResult.Failure>(client.fetch(listOf(sannomiya)))

        assertEquals(WeatherFetchFailureReason.INVALID_RESPONSE, failure.reason)
    }

    @Test
    fun `地点が 0 件や 11 件以上なら呼ばずに例外にする`() {
        assertFailsWith<IllegalArgumentException> { client.fetch(emptyList()) }
        assertFailsWith<IllegalArgumentException> { client.fetch(List(11) { sannomiya }) }
        server.verify()
    }

    private companion object {
        const val OBSERVATION_ZERO = """{"Type":"observation","Date":"202611101345","Rainfall":0.0}"""

        /** 三宮・ハーバーランドの順で返す応答（`Geometry.Coordinates` は Yahoo! と同じ「経度,緯度」）。 */
        val FEATURE_COORDINATES = listOf("135.195500,34.691300", "135.180000,34.680000")

        fun featureJson(vararg weatherLists: String): String =
            featureJsonWithCoordinates(weatherLists.mapIndexed { i, weathers -> FEATURE_COORDINATES[i] to weathers })

        /** Feature ごとに `Geometry.Coordinates`（null なら Geometry なし）と Weather を指定する。 */
        fun featureJsonWithCoordinates(features: List<Pair<String?, String>>): String {
            val featureJsons =
                features.joinToString(",") { (coordinates, weathers) ->
                    val geometry = coordinates?.let { """"Geometry":{"Type":"point","Coordinates":"$it"},""" }.orEmpty()
                    """{$geometry"Property":{"WeatherList":{"Weather":[$weathers]}}}"""
                }
            return """{"ResultInfo":{"Count":${features.size},"Status":200},"Feature":[$featureJsons]}"""
        }

        /** 実際の応答（2026-11-10 に取得）と同じ形。 */
        val TWO_POINTS_JSON =
            """
            {
              "ResultInfo": {"Count": 2, "Total": 2, "Start": 1, "Status": 200, "Latency": 0.036, "Description": ""},
              "Feature": [
                {
                  "Id": "202611101345_135.195500_34.691300",
                  "Geometry": {"Type": "point", "Coordinates": "135.195500,34.691300"},
                  "Property": {
                    "WeatherAreaCode": 6310,
                    "WeatherList": {"Weather": [
                      {"Type": "observation", "Date": "202611101345", "Rainfall": 1.65},
                      {"Type": "forecast", "Date": "202611101355", "Rainfall": 0.0},
                      {"Type": "forecast", "Date": "202611101405", "Rainfall": 0.85}
                    ]}
                  }
                },
                {
                  "Id": "202611101345_135.180000_34.680000",
                  "Geometry": {"Type": "point", "Coordinates": "135.180000,34.680000"},
                  "Property": {
                    "WeatherAreaCode": 6310,
                    "WeatherList": {"Weather": [
                      {"Type": "observation", "Date": "202611101345", "Rainfall": 0.0}
                    ]}
                  }
                }
              ]
            }
            """.trimIndent()
    }
}

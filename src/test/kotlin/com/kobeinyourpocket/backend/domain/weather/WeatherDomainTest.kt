package com.kobeinyourpocket.backend.domain.weather

import com.kobeinyourpocket.backend.domain.weather.model.PointWeather
import com.kobeinyourpocket.backend.domain.weather.vo.RainfallReading
import com.kobeinyourpocket.backend.domain.weather.vo.RainfallReadingType
import com.kobeinyourpocket.backend.domain.weather.vo.WeatherPoint
import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class WeatherDomainTest {
    @Test
    fun `緯度経度が範囲外なら作れない`() {
        assertFailsWith<IllegalArgumentException> { WeatherPoint(latitude = 91.0, longitude = 135.0) }
        assertFailsWith<IllegalArgumentException> { WeatherPoint(latitude = -90.01, longitude = 135.0) }
        assertFailsWith<IllegalArgumentException> { WeatherPoint(latitude = 34.0, longitude = 181.0) }
        assertFailsWith<IllegalArgumentException> { WeatherPoint(latitude = 34.0, longitude = -180.01) }
    }

    @Test
    fun `緯度経度が NaN なら作れない`() {
        assertFailsWith<IllegalArgumentException> { WeatherPoint(latitude = Double.NaN, longitude = 135.0) }
        assertFailsWith<IllegalArgumentException> { WeatherPoint(latitude = 34.0, longitude = Double.NaN) }
    }

    @Test
    fun `緯度経度の境界値ちょうどなら作れる`() {
        WeatherPoint(latitude = 90.0, longitude = 180.0)
        WeatherPoint(latitude = -90.0, longitude = -180.0)
    }

    @Test
    fun `範囲内の緯度経度なら作れる`() {
        val point = WeatherPoint(latitude = 34.6913, longitude = 135.1955)

        assertEquals(34.6913, point.latitude)
    }

    @Test
    fun `降水強度は負の値にできない`() {
        assertFailsWith<IllegalArgumentException> {
            RainfallReading(RainfallReadingType.OBSERVATION, Instant.EPOCH, mmPerHour = -0.01)
        }
    }

    @Test
    fun `降水強度は NaN や無限大にできない`() {
        listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY).forEach { value ->
            assertFailsWith<IllegalArgumentException>("mmPerHour=$value") {
                RainfallReading(RainfallReadingType.OBSERVATION, Instant.EPOCH, mmPerHour = value)
            }
        }
    }

    @Test
    fun `降水強度は 0 以上なら作れる`() {
        val reading = RainfallReading(RainfallReadingType.FORECAST, Instant.EPOCH, mmPerHour = 0.0)

        assertEquals(0.0, reading.mmPerHour)
    }

    private val kobe = WeatherPoint(latitude = 34.690083, longitude = 135.195511)
    private val t0 = Instant.parse("2026-11-11T04:45:00Z")

    private fun reading(
        type: RainfallReadingType,
        minutesLater: Long,
    ) = RainfallReading(type, t0.plus(Duration.ofMinutes(minutesLater)), mmPerHour = 0.0)

    @Test
    fun `観測値 1 件と予測値を時刻順に持つ天気は作れる`() {
        val weather =
            PointWeather(
                kobe,
                listOf(
                    reading(RainfallReadingType.OBSERVATION, 0),
                    reading(RainfallReadingType.FORECAST, 10),
                    reading(RainfallReadingType.FORECAST, 20),
                ),
            )

        assertEquals(3, weather.readings.size)
    }

    @Test
    fun `観測値だけの天気は作れる`() {
        PointWeather(kobe, listOf(reading(RainfallReadingType.OBSERVATION, 0)))
    }

    @Test
    fun `観測値が無い天気（空・予測値だけ）は作れない`() {
        assertFailsWith<IllegalArgumentException> { PointWeather(kobe, emptyList()) }
        assertFailsWith<IllegalArgumentException> {
            PointWeather(kobe, listOf(reading(RainfallReadingType.FORECAST, 10), reading(RainfallReadingType.FORECAST, 20)))
        }
    }

    @Test
    fun `観測値が 2 件ある天気は作れない`() {
        assertFailsWith<IllegalArgumentException> {
            PointWeather(kobe, listOf(reading(RainfallReadingType.OBSERVATION, 0), reading(RainfallReadingType.OBSERVATION, 10)))
        }
    }

    @Test
    fun `時刻順に並んでいない天気は作れない`() {
        assertFailsWith<IllegalArgumentException> {
            PointWeather(kobe, listOf(reading(RainfallReadingType.FORECAST, 10), reading(RainfallReadingType.OBSERVATION, 0)))
        }
    }

    @Test
    fun `同じ時刻の値が 2 件ある天気は作れない`() {
        assertFailsWith<IllegalArgumentException> {
            PointWeather(kobe, listOf(reading(RainfallReadingType.OBSERVATION, 0), reading(RainfallReadingType.FORECAST, 0)))
        }
    }
}

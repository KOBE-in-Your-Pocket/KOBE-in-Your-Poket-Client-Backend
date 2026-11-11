package com.kobeinyourpocket.backend.domain.weather

import com.kobeinyourpocket.backend.domain.weather.vo.RainfallReading
import com.kobeinyourpocket.backend.domain.weather.vo.RainfallReadingType
import com.kobeinyourpocket.backend.domain.weather.vo.WeatherPoint
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class WeatherDomainTest {
    @Test
    fun `緯度経度が範囲外なら作れない`() {
        assertFailsWith<IllegalArgumentException> { WeatherPoint(latitude = 91.0, longitude = 135.0) }
        assertFailsWith<IllegalArgumentException> { WeatherPoint(latitude = 34.0, longitude = 181.0) }
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
    fun `降水強度は 0 以上なら作れる`() {
        val reading = RainfallReading(RainfallReadingType.FORECAST, Instant.EPOCH, mmPerHour = 0.0)

        assertEquals(0.0, reading.mmPerHour)
    }

    @Test
    fun `神戸の中心の地点は神戸市役所付近を指す`() {
        val kobe = WeatherPoint.KOBE_CENTER

        assertEquals(34.690083, kobe.latitude)
        assertEquals(135.195511, kobe.longitude)
    }
}

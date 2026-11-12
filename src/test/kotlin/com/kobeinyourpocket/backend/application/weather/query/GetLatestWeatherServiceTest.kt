package com.kobeinyourpocket.backend.application.weather.query

import com.kobeinyourpocket.backend.domain.weather.vo.WeatherPoint
import io.mockk.every
import io.mockk.mockk
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GetLatestWeatherServiceTest {
    private val query = mockk<WeatherQuery>()
    private val service = GetLatestWeatherService(query)

    @Test
    fun `神戸の中心の最新の天気を返す`() {
        val view =
            LatestWeatherView(
                latitude = WeatherPoint.KOBE_CENTER.latitude,
                longitude = WeatherPoint.KOBE_CENTER.longitude,
                fetchedAt = Instant.parse("2026-11-11T05:39:55Z"),
                observation = RainfallView(Instant.parse("2026-11-11T05:35:00Z"), 0.0),
                forecasts = emptyList(),
            )
        every { query.findLatest(WeatherPoint.KOBE_CENTER) } returns view

        assertEquals(view, service.getLatest())
    }

    @Test
    fun `まだ取得できていなければ null を返す`() {
        every { query.findLatest(WeatherPoint.KOBE_CENTER) } returns null

        assertNull(service.getLatest())
    }
}

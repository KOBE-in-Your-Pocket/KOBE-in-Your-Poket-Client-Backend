package com.kobeinyourpocket.backend.application.weather.command

import com.kobeinyourpocket.backend.application.weather.WeatherFetchFailureReason
import com.kobeinyourpocket.backend.application.weather.WeatherFetchResult
import com.kobeinyourpocket.backend.application.weather.WeatherGateway
import com.kobeinyourpocket.backend.domain.weather.model.PointWeather
import com.kobeinyourpocket.backend.domain.weather.model.WeatherSnapshot
import com.kobeinyourpocket.backend.domain.weather.repository.WeatherRepository
import com.kobeinyourpocket.backend.domain.weather.vo.RainfallReading
import com.kobeinyourpocket.backend.domain.weather.vo.RainfallReadingType
import com.kobeinyourpocket.backend.domain.weather.vo.WeatherPoint
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** 天気の取得と保存（#232）。気象情報 API と DB は MockK で差し替える。 */
class RefreshWeatherServiceTest {
    private val gateway = mockk<WeatherGateway>()
    private val repository = mockk<WeatherRepository>()
    private val now = Instant.parse("2026-11-11T05:00:30Z")
    private val service = RefreshWeatherService(gateway, repository, Clock.fixed(now, ZoneOffset.UTC))

    private val kobeWeather =
        PointWeather(
            WeatherPoint.KOBE_CENTER,
            listOf(RainfallReading(RainfallReadingType.OBSERVATION, Instant.parse("2026-11-11T05:00:00Z"), 1.5)),
        )

    @Test
    fun `取得できたら、神戸の中心の天気を取得時刻つきで保存し直す`() {
        every { gateway.isConfigured } returns true
        every { gateway.fetch(listOf(WeatherPoint.KOBE_CENTER)) } returns WeatherFetchResult.Success(listOf(kobeWeather))
        every { repository.replace(any()) } just runs

        val result = service.execute()

        assertEquals(RefreshWeatherResult.REFRESHED, result)
        verify(exactly = 1) { repository.replace(WeatherSnapshot(kobeWeather, now)) }
    }

    @Test
    fun `取得できなかったら保存せず、前回のデータを残す`() {
        every { gateway.isConfigured } returns true
        listOf(
            WeatherFetchFailureReason.API_ERROR,
            WeatherFetchFailureReason.NETWORK_ERROR,
            WeatherFetchFailureReason.INVALID_RESPONSE,
        ).forEach { reason ->
            every { gateway.fetch(any()) } returns WeatherFetchResult.Failure(reason, "test")

            assertEquals(RefreshWeatherResult.FAILED, service.execute(), "$reason")
        }

        verify(exactly = 0) { repository.replace(any()) }
    }

    @Test
    fun `Client ID が未設定なら取得も保存もしない`() {
        every { gateway.isConfigured } returns false

        val result = service.execute()

        assertEquals(RefreshWeatherResult.SKIPPED_NOT_CONFIGURED, result)
        verify(exactly = 0) { gateway.fetch(any()) }
        verify(exactly = 0) { repository.replace(any()) }
    }

    @Test
    fun `保存に失敗したら例外を呼び出し元へ返す`() {
        every { gateway.isConfigured } returns true
        every { gateway.fetch(any()) } returns WeatherFetchResult.Success(listOf(kobeWeather))
        every { repository.replace(any()) } throws IllegalStateException("db down")

        assertFailsWith<IllegalStateException> { service.execute() }
    }
}

package com.kobeinyourpocket.backend.infrastructure.weather

import com.kobeinyourpocket.backend.application.weather.command.RefreshWeatherResult
import com.kobeinyourpocket.backend.application.weather.command.RefreshWeatherService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** 天気の取得バッチ（#232）。 */
class WeatherRefreshSchedulerTest {
    private val service = mockk<RefreshWeatherService>()

    @Test
    fun `実行すると天気の取得と保存を 1 回呼ぶ`() {
        every { service.execute() } returns RefreshWeatherResult.REFRESHED

        WeatherRefreshScheduler(service).refresh()

        verify(exactly = 1) { service.execute() }
    }

    @Test
    fun `保存に失敗しても例外を外へ出さない（次の周期も実行できるように）`() {
        every { service.execute() } throws IllegalStateException("db down")

        WeatherRefreshScheduler(service).refresh()

        verify(exactly = 1) { service.execute() }
    }

    private val contextRunner =
        ApplicationContextRunner()
            .withBean(RefreshWeatherService::class.java, { service })
            .withUserConfiguration(WeatherSchedulingConfig::class.java, WeatherRefreshScheduler::class.java)

    @Test
    fun `設定が無ければバッチとスケジューラを作る`() {
        contextRunner.run { context ->
            assertEquals(1, context.getBeansOfType(WeatherRefreshScheduler::class.java).size)
            assertEquals(1, context.getBeansOfType(WeatherSchedulingConfig::class.java).size)
        }
    }

    @Test
    fun `weather_refresh_enabled=false ならバッチもスケジューラも作らない`() {
        contextRunner.withPropertyValues("weather.refresh.enabled=false").run { context ->
            assertTrue(context.getBeansOfType(WeatherRefreshScheduler::class.java).isEmpty())
            assertTrue(context.getBeansOfType(WeatherSchedulingConfig::class.java).isEmpty())
        }
    }
}

package com.kobeinyourpocket.backend.infrastructure.rest.weather

import com.kobeinyourpocket.backend.domain.weather.model.PointWeather
import com.kobeinyourpocket.backend.domain.weather.model.WeatherSnapshot
import com.kobeinyourpocket.backend.domain.weather.repository.WeatherRepository
import com.kobeinyourpocket.backend.domain.weather.vo.RainfallReading
import com.kobeinyourpocket.backend.domain.weather.vo.RainfallReadingType
import com.kobeinyourpocket.backend.domain.weather.vo.WeatherPoint
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import kotlin.test.Test

/**
 * 最新の天気 API の統合テスト（#233）。controller → application → JPA → DB を実 Bean で通し、
 * 認証なしで取得できること（SecurityConfig の閲覧系 permitAll）も確かめる。
 *
 * データ投入は [WeatherRepository]（write port）で行う。各テストは [Transactional] でロールバックする。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WeatherApiIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var weatherRepository: WeatherRepository

    @Test
    fun `認証なしで、保存されている神戸の中心の天気を返す`() {
        val observedAt = Instant.parse("2026-11-11T05:35:00Z")
        weatherRepository.replace(
            WeatherSnapshot(
                PointWeather(
                    WeatherPoint.KOBE_CENTER,
                    listOf(
                        RainfallReading(RainfallReadingType.OBSERVATION, observedAt, 2.0),
                        RainfallReading(RainfallReadingType.FORECAST, Instant.parse("2026-11-11T05:45:00Z"), 4.5),
                    ),
                ),
                Instant.parse("2026-11-11T05:39:55Z"),
            ),
        )

        mockMvc
            .perform(get("/api/v1/weather"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.observation.mmPerHour").value(2.0))
            .andExpect(jsonPath("$.forecasts[0].mmPerHour").value(4.5))
            .andExpect(jsonPath("$.fetchedAt").value("2026-11-11T05:39:55Z"))
    }

    @Test
    fun `認証なしで、まだ天気が無ければ 204 を返す`() {
        mockMvc
            .perform(get("/api/v1/weather"))
            .andExpect(status().isNoContent)
    }
}

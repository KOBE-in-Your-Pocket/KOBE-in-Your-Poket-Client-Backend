package com.kobeinyourpocket.backend.infrastructure.rest.weather

import com.kobeinyourpocket.backend.application.weather.query.GetLatestWeatherService
import com.kobeinyourpocket.backend.application.weather.query.LatestWeatherView
import com.kobeinyourpocket.backend.application.weather.query.RainfallView
import com.kobeinyourpocket.backend.infrastructure.rest.common.GlobalExceptionHandler
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
import kotlin.test.Test

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(WeatherController::class)
@Import(GlobalExceptionHandler::class)
class WeatherControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var getLatestWeatherService: GetLatestWeatherService

    @Test
    fun `最新の天気を観測値と予測値に分けて返す`() {
        given(getLatestWeatherService.getLatest()).willReturn(
            LatestWeatherView(
                latitude = 34.690083,
                longitude = 135.195511,
                fetchedAt = Instant.parse("2026-11-11T05:39:55Z"),
                observation = RainfallView(Instant.parse("2026-11-11T05:35:00Z"), 1.65),
                forecasts =
                    listOf(
                        RainfallView(Instant.parse("2026-11-11T05:45:00Z"), 0.85),
                        RainfallView(Instant.parse("2026-11-11T05:55:00Z"), 0.0),
                    ),
            ),
        )

        mockMvc
            .perform(get("/api/v1/weather"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.latitude").value(34.690083))
            .andExpect(jsonPath("$.longitude").value(135.195511))
            .andExpect(jsonPath("$.fetchedAt").value("2026-11-11T05:39:55Z"))
            .andExpect(jsonPath("$.observation.at").value("2026-11-11T05:35:00Z"))
            .andExpect(jsonPath("$.observation.mmPerHour").value(1.65))
            .andExpect(jsonPath("$.forecasts.length()").value(2))
            .andExpect(jsonPath("$.forecasts[0].at").value("2026-11-11T05:45:00Z"))
            .andExpect(jsonPath("$.forecasts[0].mmPerHour").value(0.85))
            .andExpect(jsonPath("$.forecasts[1].mmPerHour").value(0.0))
    }

    @Test
    fun `まだ天気が無ければ 204 で本文なしを返す`() {
        given(getLatestWeatherService.getLatest()).willReturn(null)

        mockMvc
            .perform(get("/api/v1/weather"))
            .andExpect(status().isNoContent)
            .andExpect(content().string(""))
    }
}

package com.kobeinyourpocket.backend.infrastructure.query.weather

import com.kobeinyourpocket.backend.application.weather.query.RainfallView
import com.kobeinyourpocket.backend.application.weather.query.WeatherQuery
import com.kobeinyourpocket.backend.domain.weather.model.PointWeather
import com.kobeinyourpocket.backend.domain.weather.model.WeatherSnapshot
import com.kobeinyourpocket.backend.domain.weather.repository.WeatherRepository
import com.kobeinyourpocket.backend.domain.weather.vo.RainfallReading
import com.kobeinyourpocket.backend.domain.weather.vo.RainfallReadingType
import com.kobeinyourpocket.backend.domain.weather.vo.WeatherPoint
import com.kobeinyourpocket.backend.infrastructure.persistence.weather.WeatherRepositoryImpl
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** 最新の天気の読み取り（#233）。データは write port（WeatherRepository）で入れる。 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(WeatherQueryJpa::class, WeatherRepositoryImpl::class)
class WeatherQueryJpaTest {
    @Autowired
    private lateinit var query: WeatherQuery

    @Autowired
    private lateinit var repository: WeatherRepository

    private val kobe = WeatherPoint.KOBE_CENTER
    private val observedAt = Instant.parse("2026-11-11T05:35:00Z")
    private val fetchedAt = Instant.parse("2026-11-11T05:39:55Z")

    private fun save(
        point: WeatherPoint = kobe,
        observation: Double = 1.5,
        forecasts: List<Double> = listOf(0.5, 0.0),
    ) {
        val readings =
            listOf(RainfallReading(RainfallReadingType.OBSERVATION, observedAt, observation)) +
                forecasts.mapIndexed { i, mm ->
                    RainfallReading(RainfallReadingType.FORECAST, observedAt.plus(Duration.ofMinutes(10L * (i + 1))), mm)
                }
        repository.replace(WeatherSnapshot(PointWeather(point, readings), fetchedAt))
    }

    @Test
    fun `観測値と予測値を分けて、予測値は時刻順に返す`() {
        save(observation = 1.5, forecasts = listOf(0.5, 0.0))

        val latest = query.findLatest(kobe)!!

        assertEquals(fetchedAt, latest.fetchedAt)
        assertEquals(RainfallView(observedAt, 1.5), latest.observation)
        assertEquals(
            listOf(
                RainfallView(observedAt.plus(Duration.ofMinutes(10)), 0.5),
                RainfallView(observedAt.plus(Duration.ofMinutes(20)), 0.0),
            ),
            latest.forecasts,
        )
    }

    @Test
    fun `予測値が無くても観測値だけで返す`() {
        save(forecasts = emptyList())

        val latest = query.findLatest(kobe)!!

        assertEquals(emptyList(), latest.forecasts)
    }

    @Test
    fun `まだ保存していなければ null を返す`() {
        assertNull(query.findLatest(kobe))
    }

    @Test
    fun `ほかの地点の天気は返さない`() {
        save(point = WeatherPoint(latitude = 34.68, longitude = 135.18))

        assertNull(query.findLatest(kobe))
    }
}

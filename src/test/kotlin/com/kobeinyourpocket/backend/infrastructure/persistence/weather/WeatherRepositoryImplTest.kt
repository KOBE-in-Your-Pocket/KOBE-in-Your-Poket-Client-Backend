package com.kobeinyourpocket.backend.infrastructure.persistence.weather

import com.kobeinyourpocket.backend.domain.weather.model.PointWeather
import com.kobeinyourpocket.backend.domain.weather.model.WeatherSnapshot
import com.kobeinyourpocket.backend.domain.weather.repository.WeatherRepository
import com.kobeinyourpocket.backend.domain.weather.vo.RainfallReading
import com.kobeinyourpocket.backend.domain.weather.vo.RainfallReadingType
import com.kobeinyourpocket.backend.domain.weather.vo.WeatherPoint
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

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(WeatherRepositoryImpl::class)
class WeatherRepositoryImplTest {
    @Autowired
    private lateinit var repository: WeatherRepository

    @Autowired
    private lateinit var jpa: WeatherReadingJpaRepository

    private val kobe = WeatherPoint.KOBE_CENTER
    private val otherPoint = WeatherPoint(latitude = 34.68, longitude = 135.18)
    private val fetchedAt = Instant.parse("2026-11-11T04:45:30Z")
    private val observedAt = Instant.parse("2026-11-11T04:45:00Z")

    /** 観測 1 件＋10 分ごとの予測 [forecasts] 件。 */
    private fun snapshot(
        point: WeatherPoint = kobe,
        observedAt: Instant = this.observedAt,
        fetchedAt: Instant = this.fetchedAt,
        forecasts: Int = 6,
        mmPerHour: Double = 0.0,
    ): WeatherSnapshot {
        val observation = RainfallReading(RainfallReadingType.OBSERVATION, observedAt, mmPerHour)
        val forecastReadings =
            (1..forecasts).map {
                RainfallReading(RainfallReadingType.FORECAST, observedAt.plus(Duration.ofMinutes(10L * it)), mmPerHour)
            }
        return WeatherSnapshot(PointWeather(point, listOf(observation) + forecastReadings), fetchedAt)
    }

    @Test
    fun `保存した天気を時刻順に読み出せる`() {
        repository.replace(snapshot(mmPerHour = 1.65))

        val latest = repository.findLatest(kobe)!!

        assertEquals(kobe, latest.weather.point)
        assertEquals(fetchedAt, latest.fetchedAt)
        assertEquals(7, latest.weather.readings.size)
        assertEquals(
            RainfallReadingType.OBSERVATION,
            latest.weather.readings
                .first()
                .type,
        )
        assertEquals(
            observedAt,
            latest.weather.readings
                .first()
                .at,
        )
        assertEquals(
            1.65,
            latest.weather.readings
                .first()
                .mmPerHour,
        )
        assertEquals(latest.weather.readings.sortedBy { it.at }, latest.weather.readings)
    }

    @Test
    fun `保存し直すと前回の取得分は消え、最新の取得分だけが残る`() {
        repository.replace(snapshot())
        val nextObservedAt = observedAt.plus(Duration.ofMinutes(15))
        val nextFetchedAt = fetchedAt.plus(Duration.ofMinutes(15))

        repository.replace(snapshot(observedAt = nextObservedAt, fetchedAt = nextFetchedAt, mmPerHour = 3.0))

        val latest = repository.findLatest(kobe)!!
        assertEquals(nextFetchedAt, latest.fetchedAt)
        assertEquals(
            nextObservedAt,
            latest.weather.readings
                .first()
                .at,
        )
        assertEquals(7, latest.weather.readings.size)
        assertEquals(
            setOf(3.0),
            latest.weather.readings
                .map { it.mmPerHour }
                .toSet(),
        )
        assertEquals(7, jpa.count())
    }

    @Test
    fun `予測が前回より少なくても、前回の余った行は残らない`() {
        repository.replace(snapshot(forecasts = 6))

        repository.replace(snapshot(forecasts = 2))

        assertEquals(
            3,
            repository
                .findLatest(kobe)!!
                .weather.readings.size,
        )
    }

    @Test
    fun `ほかの地点の天気は置き換えない`() {
        repository.replace(snapshot(point = otherPoint))

        repository.replace(snapshot(point = kobe))

        assertEquals(
            7,
            repository
                .findLatest(otherPoint)!!
                .weather.readings.size,
        )
        assertEquals(14, jpa.count())
    }

    @Test
    fun `まだ保存していない地点は null を返す`() {
        assertNull(repository.findLatest(kobe))
    }
}

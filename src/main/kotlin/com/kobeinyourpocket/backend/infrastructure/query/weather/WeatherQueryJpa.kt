package com.kobeinyourpocket.backend.infrastructure.query.weather

import com.kobeinyourpocket.backend.application.weather.query.LatestWeatherView
import com.kobeinyourpocket.backend.application.weather.query.RainfallView
import com.kobeinyourpocket.backend.application.weather.query.WeatherQuery
import com.kobeinyourpocket.backend.domain.weather.vo.WeatherPoint
import com.kobeinyourpocket.backend.infrastructure.query.common.JdbcTimestamps
import jakarta.persistence.EntityManager
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.time.Instant

/**
 * [WeatherQuery] の JPA 実装（#233）。`weather_readings` から地点の行を時刻順に読む。
 *
 * 地点ごとに最新の取得分だけを保存しているため（#231）、地点の行をすべて読めば最新の天気になる。
 */
@Repository
class WeatherQueryJpa(
    private val entityManager: EntityManager,
) : WeatherQuery {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun findLatest(point: WeatherPoint): LatestWeatherView? {
        @Suppress("UNCHECKED_CAST")
        val rows =
            entityManager
                .createNativeQuery(SELECT_READINGS)
                .setParameter("latitude", point.latitude)
                .setParameter("longitude", point.longitude)
                .resultList as List<Array<Any?>>
        if (rows.isEmpty()) return null

        val readings = rows.map(::toReading)
        val observation = readings.singleOrNull { it.type == OBSERVATION }
        if (observation == null) {
            // 保存時に観測値ちょうど 1 件を守っているため（PointWeather の不変条件）、通常は起きない。
            // データ不整合でも API を 500 にせず、「天気なし」として返す。
            logger.warn("weather_readings に観測値がちょうど 1 件ない（point={}, rows={}）", point, rows.size)
            return null
        }
        return LatestWeatherView(
            latitude = point.latitude,
            longitude = point.longitude,
            fetchedAt = readings.maxOf { it.fetchedAt },
            observation = observation.toView(),
            forecasts = readings.filter { it.type == FORECAST }.map { it.toView() },
        )
    }

    private fun toReading(row: Array<Any?>): Reading =
        Reading(
            type = row[Column.READING_TYPE] as String,
            at = JdbcTimestamps.toInstant(row[Column.TARGET_AT]),
            mmPerHour = (row[Column.MM_PER_HOUR] as Number).toDouble(),
            fetchedAt = JdbcTimestamps.toInstant(row[Column.FETCHED_AT]),
        )

    private data class Reading(
        val type: String,
        val at: Instant,
        val mmPerHour: Double,
        val fetchedAt: Instant,
    ) {
        fun toView() = RainfallView(at = at, mmPerHour = mmPerHour)
    }

    /** [SELECT_READINGS] の列順と対応する index。列の並び替え時は両方を合わせて更新すること。 */
    private object Column {
        const val READING_TYPE = 0
        const val TARGET_AT = 1
        const val MM_PER_HOUR = 2
        const val FETCHED_AT = 3
    }

    private companion object {
        const val OBSERVATION = "observation"
        const val FORECAST = "forecast"

        val SELECT_READINGS =
            """
            SELECT reading_type, target_at, mm_per_hour, fetched_at
            FROM weather_readings
            WHERE latitude = :latitude AND longitude = :longitude
            ORDER BY target_at
            """.trimIndent()
    }
}

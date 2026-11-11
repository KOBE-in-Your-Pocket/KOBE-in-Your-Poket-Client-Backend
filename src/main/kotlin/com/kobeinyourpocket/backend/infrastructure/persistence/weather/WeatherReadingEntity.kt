package com.kobeinyourpocket.backend.infrastructure.persistence.weather

import com.kobeinyourpocket.backend.domain.weather.vo.RainfallReading
import com.kobeinyourpocket.backend.domain.weather.vo.RainfallReadingType
import com.kobeinyourpocket.backend.domain.weather.vo.WeatherPoint
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.io.Serializable
import java.time.Instant

/** `weather_readings` の複合主キー (latitude, longitude, target_at)。1 地点・1 時刻に 1 行。 */
@Embeddable
data class WeatherReadingId(
    @Column(name = "latitude", nullable = false)
    val latitude: Double,
    @Column(name = "longitude", nullable = false)
    val longitude: Double,
    @Column(name = "target_at", nullable = false)
    val targetAt: Instant,
) : Serializable

/** DB `weather_readings`（V24 / #231）。1 行 = ある地点の、ある時刻の降水強度。 */
@Entity
@Table(name = "weather_readings")
class WeatherReadingEntity(
    @EmbeddedId
    var id: WeatherReadingId,
    @Column(name = "reading_type", nullable = false, length = 16)
    var readingType: String,
    @Column(name = "mm_per_hour", nullable = false)
    var mmPerHour: Double,
    @Column(name = "fetched_at", nullable = false)
    var fetchedAt: Instant,
) {
    fun toReading(): RainfallReading =
        RainfallReading(
            type = typeOf(readingType),
            at = id.targetAt,
            mmPerHour = mmPerHour,
        )

    companion object {
        private const val OBSERVATION = "observation"
        private const val FORECAST = "forecast"

        fun of(
            point: WeatherPoint,
            reading: RainfallReading,
            fetchedAt: Instant,
        ): WeatherReadingEntity =
            WeatherReadingEntity(
                id = WeatherReadingId(latitude = point.latitude, longitude = point.longitude, targetAt = reading.at),
                readingType = wireValueOf(reading.type),
                mmPerHour = reading.mmPerHour,
                fetchedAt = fetchedAt,
            )

        private fun wireValueOf(type: RainfallReadingType): String =
            when (type) {
                RainfallReadingType.OBSERVATION -> OBSERVATION
                RainfallReadingType.FORECAST -> FORECAST
            }

        private fun typeOf(value: String): RainfallReadingType =
            when (value) {
                OBSERVATION -> RainfallReadingType.OBSERVATION
                FORECAST -> RainfallReadingType.FORECAST
                // DB の CHECK 制約で入らないはずの値。入っていたらデータ不整合として落とす。
                else -> error("unknown reading_type in weather_readings: $value")
            }
    }
}

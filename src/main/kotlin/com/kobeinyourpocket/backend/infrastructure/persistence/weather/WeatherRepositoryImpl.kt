package com.kobeinyourpocket.backend.infrastructure.persistence.weather

import com.kobeinyourpocket.backend.domain.weather.model.PointWeather
import com.kobeinyourpocket.backend.domain.weather.model.WeatherSnapshot
import com.kobeinyourpocket.backend.domain.weather.repository.WeatherRepository
import com.kobeinyourpocket.backend.domain.weather.vo.WeatherPoint
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

/** [WeatherRepository] port の outbound adapter（#231）。 */
@Repository
class WeatherRepositoryImpl(
    private val weatherReadingJpa: WeatherReadingJpaRepository,
) : WeatherRepository {
    /** 前回分の削除と今回分の保存を 1 トランザクションで行う。保存に失敗したら削除も取り消され、前回分が残る。 */
    @Transactional
    override fun replace(snapshot: WeatherSnapshot) {
        val point = snapshot.weather.point
        weatherReadingJpa.deleteByPoint(point.latitude, point.longitude)
        weatherReadingJpa.saveAll(
            snapshot.weather.readings.map { WeatherReadingEntity.of(point, it, snapshot.fetchedAt) },
        )
    }

    @Transactional(readOnly = true)
    override fun findLatest(point: WeatherPoint): WeatherSnapshot? {
        val rows = weatherReadingJpa.findByPoint(point.latitude, point.longitude)
        if (rows.isEmpty()) return null
        return WeatherSnapshot(
            weather = PointWeather(point = point, readings = rows.map { it.toReading() }),
            // 同じ取得分の行はすべて同じ fetched_at を持つ。
            fetchedAt = rows.maxOf { it.fetchedAt },
        )
    }
}

package com.kobeinyourpocket.backend.infrastructure.persistence.weather

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface WeatherReadingJpaRepository : JpaRepository<WeatherReadingEntity, WeatherReadingId> {
    /** 地点の行を時刻順に返す。 */
    @Query(
        "SELECT r FROM WeatherReadingEntity r " +
            "WHERE r.id.latitude = :latitude AND r.id.longitude = :longitude ORDER BY r.id.targetAt",
    )
    fun findByPoint(
        @Param("latitude") latitude: Double,
        @Param("longitude") longitude: Double,
    ): List<WeatherReadingEntity>

    /**
     * 地点の行をすべて消す。
     *
     * 1 件ずつ読み込んで消さないよう、JPQL の一括削除にする。
     * 一括削除は永続化コンテキストを通らないため、前後で flush / clear する。clear しないと、
     * 同じトランザクションで同じ主キーの行を入れ直したときに、消した行の entity が残っていて
     * INSERT ではなく UPDATE になり、行が入らない。
     *
     * @return 削除した件数
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM WeatherReadingEntity r WHERE r.id.latitude = :latitude AND r.id.longitude = :longitude")
    fun deleteByPoint(
        @Param("latitude") latitude: Double,
        @Param("longitude") longitude: Double,
    ): Int
}

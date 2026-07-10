package com.kobeinyourpocket.backend.infrastructure.persistence.evacuation.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.time.LocalDate

/**
 * DB `shelter_dataset_metadata`（シングルトン、id=1固定 / #66）。
 *
 * 書き込みは行わない（本番の更新は Flyway シード時のトリガー経由 / #85）。
 * この Entity は Hibernate の schema 検証・テスト用 H2 schema 生成のためだけに存在する。
 */
@Entity
@Table(name = "shelter_dataset_metadata")
class ShelterDatasetMetadataEntity(
    @Id
    @Column(name = "id")
    var id: Short,
    @Column(name = "source", nullable = false)
    var source: String,
    @Column(name = "as_of", nullable = false)
    var asOf: LocalDate,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,
) {
    companion object {
        const val SINGLETON_ID: Short = 1
    }
}

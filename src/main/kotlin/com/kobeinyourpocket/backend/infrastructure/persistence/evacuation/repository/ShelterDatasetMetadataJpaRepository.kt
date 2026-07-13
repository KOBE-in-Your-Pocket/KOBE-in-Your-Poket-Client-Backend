package com.kobeinyourpocket.backend.infrastructure.persistence.evacuation.repository

import com.kobeinyourpocket.backend.infrastructure.persistence.evacuation.entity.ShelterDatasetMetadataEntity
import org.springframework.data.jpa.repository.JpaRepository

/** 本番での書き込みは無い。テストでの seed 用（#85）。 */
interface ShelterDatasetMetadataJpaRepository : JpaRepository<ShelterDatasetMetadataEntity, Short>

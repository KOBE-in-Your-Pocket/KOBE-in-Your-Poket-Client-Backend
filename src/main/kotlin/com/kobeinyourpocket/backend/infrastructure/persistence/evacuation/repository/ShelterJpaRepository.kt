package com.kobeinyourpocket.backend.infrastructure.persistence.evacuation.repository

import com.kobeinyourpocket.backend.infrastructure.persistence.evacuation.entity.ShelterEntity
import com.kobeinyourpocket.backend.infrastructure.persistence.evacuation.entity.ShelterLocalizationEntity
import com.kobeinyourpocket.backend.infrastructure.persistence.evacuation.entity.ShelterLocalizationId
import org.springframework.data.jpa.repository.JpaRepository

interface ShelterJpaRepository : JpaRepository<ShelterEntity, String>

interface ShelterLocalizationJpaRepository : JpaRepository<ShelterLocalizationEntity, ShelterLocalizationId> {
    fun deleteByIdShelterId(shelterId: String)
}

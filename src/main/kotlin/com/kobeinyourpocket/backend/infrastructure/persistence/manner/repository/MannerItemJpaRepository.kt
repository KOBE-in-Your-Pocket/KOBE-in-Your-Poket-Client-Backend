package com.kobeinyourpocket.backend.infrastructure.persistence.manner.repository

import com.kobeinyourpocket.backend.infrastructure.persistence.manner.entity.MannerItemEntity
import com.kobeinyourpocket.backend.infrastructure.persistence.manner.entity.MannerItemLocalizationEntity
import com.kobeinyourpocket.backend.infrastructure.persistence.manner.entity.MannerItemLocalizationId
import com.kobeinyourpocket.backend.infrastructure.persistence.manner.entity.MannerItemSpotEntity
import com.kobeinyourpocket.backend.infrastructure.persistence.manner.entity.MannerItemSpotId
import org.springframework.data.jpa.repository.JpaRepository

interface MannerItemJpaRepository : JpaRepository<MannerItemEntity, String>

interface MannerItemLocalizationJpaRepository : JpaRepository<MannerItemLocalizationEntity, MannerItemLocalizationId> {
    fun deleteByIdMannerItemId(mannerItemId: String)
}

interface MannerItemSpotJpaRepository : JpaRepository<MannerItemSpotEntity, MannerItemSpotId> {
    fun deleteByIdMannerItemId(mannerItemId: String)
}

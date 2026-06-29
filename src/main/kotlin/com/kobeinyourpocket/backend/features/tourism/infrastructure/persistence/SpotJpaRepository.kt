package com.kobeinyourpocket.backend.features.tourism.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface SpotJpaRepository : JpaRepository<SpotEntity, String>

interface SpotLocalizationJpaRepository : JpaRepository<SpotLocalizationEntity, SpotLocalizationId>

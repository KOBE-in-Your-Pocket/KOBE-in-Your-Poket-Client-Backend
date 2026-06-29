package com.kobeinyourpocket.backend.infrastructure.persistence.tourism

import org.springframework.data.jpa.repository.JpaRepository

interface SpotJpaRepository : JpaRepository<SpotEntity, String>

interface SpotLocalizationJpaRepository : JpaRepository<SpotLocalizationEntity, SpotLocalizationId>

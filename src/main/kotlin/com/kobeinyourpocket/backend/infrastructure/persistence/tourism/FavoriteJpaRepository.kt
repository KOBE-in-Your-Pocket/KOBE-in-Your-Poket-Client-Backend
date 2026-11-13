package com.kobeinyourpocket.backend.infrastructure.persistence.tourism

import org.springframework.data.jpa.repository.JpaRepository

interface FavoriteJpaRepository : JpaRepository<FavoriteEntity, FavoriteKey>

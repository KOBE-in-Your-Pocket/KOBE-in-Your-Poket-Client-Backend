package com.kobeinyourpocket.backend.infrastructure.persistence.tourism

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ReviewJpaRepository : JpaRepository<ReviewEntity, UUID>

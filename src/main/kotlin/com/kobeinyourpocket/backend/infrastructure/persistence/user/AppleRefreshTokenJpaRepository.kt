package com.kobeinyourpocket.backend.infrastructure.persistence.user

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AppleRefreshTokenJpaRepository : JpaRepository<AppleRefreshTokenEntity, UUID>

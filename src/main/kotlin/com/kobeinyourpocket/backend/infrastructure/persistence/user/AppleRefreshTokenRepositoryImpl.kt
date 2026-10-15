package com.kobeinyourpocket.backend.infrastructure.persistence.user

import com.kobeinyourpocket.backend.domain.user.model.User
import com.kobeinyourpocket.backend.domain.user.repository.AppleRefreshTokenRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/** [AppleRefreshTokenRepository] port の outbound adapter（#213）。 */
@Repository
class AppleRefreshTokenRepositoryImpl(
    private val jpa: AppleRefreshTokenJpaRepository,
) : AppleRefreshTokenRepository {
    @Transactional
    override fun save(
        userId: User.Id,
        refreshToken: String,
    ) {
        val now = Instant.now()
        val entity =
            jpa.findById(userId.value).orElse(null)?.apply {
                this.refreshToken = refreshToken
                this.updatedAt = now
            } ?: AppleRefreshTokenEntity(userId = userId.value, refreshToken = refreshToken, updatedAt = now)
        jpa.save(entity)
    }

    override fun findByUserId(userId: User.Id): String? = jpa.findById(userId.value).map { it.refreshToken }.orElse(null)

    @Transactional
    override fun deleteByUserId(userId: User.Id) {
        jpa.deleteById(userId.value)
    }
}

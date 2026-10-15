package com.kobeinyourpocket.backend.infrastructure.persistence.user

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/** DB `apple_refresh_tokens`。退会時の失効に使う Apple の refresh token（#213）。 */
@Entity
@Table(name = "apple_refresh_tokens")
class AppleRefreshTokenEntity(
    @Id
    @Column(name = "user_id", columnDefinition = "uuid", updatable = false, nullable = false)
    var userId: UUID,
    @Column(name = "refresh_token", nullable = false, columnDefinition = "text")
    var refreshToken: String,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,
) {
    // refresh token をログへ出さない。
    override fun toString(): String = "AppleRefreshTokenEntity(userId=$userId, updatedAt=$updatedAt)"
}

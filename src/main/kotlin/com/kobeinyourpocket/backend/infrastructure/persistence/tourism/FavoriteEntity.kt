package com.kobeinyourpocket.backend.infrastructure.persistence.tourism

import com.kobeinyourpocket.backend.domain.tourism.favorite.model.Favorite
import com.kobeinyourpocket.backend.domain.tourism.favorite.vo.FavoriteOwnerId
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.io.Serializable
import java.time.Instant
import java.util.UUID

/** `favorites` の複合主キー (user_id, spot_id)。誰がどのスポットをお気に入りしたかそのものが同一性。 */
@Embeddable
data class FavoriteKey(
    @Column(name = "user_id", columnDefinition = "uuid", nullable = false)
    val userId: UUID,
    @Column(name = "spot_id", nullable = false, length = 128)
    val spotId: String,
) : Serializable

/** DB `favorites`（V25 / #235）。 */
@Entity
@Table(name = "favorites")
class FavoriteEntity(
    @EmbeddedId
    var id: FavoriteKey,
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant,
) {
    fun toDomain(): Favorite =
        Favorite(
            ownerId = FavoriteOwnerId.of(id.userId),
            spotId = SpotId.of(id.spotId),
            createdAt = createdAt,
        )

    companion object {
        fun fromDomain(favorite: Favorite): FavoriteEntity =
            FavoriteEntity(
                id = FavoriteKey(userId = favorite.ownerId.value, spotId = favorite.spotId.value),
                createdAt = favorite.createdAt,
            )
    }
}

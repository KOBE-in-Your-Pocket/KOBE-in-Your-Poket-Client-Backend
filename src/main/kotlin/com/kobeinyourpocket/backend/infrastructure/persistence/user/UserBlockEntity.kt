package com.kobeinyourpocket.backend.infrastructure.persistence.user

import com.kobeinyourpocket.backend.domain.user.model.User
import com.kobeinyourpocket.backend.domain.user.model.UserBlock
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.io.Serializable
import java.time.Instant
import java.util.UUID

/** `user_blocks` の複合主キー (blocker_user_id, blocked_user_id)。誰が誰をブロックしたかそのものが同一性。 */
@Embeddable
data class UserBlockId(
    @Column(name = "blocker_user_id", columnDefinition = "uuid", nullable = false)
    val blockerUserId: UUID,
    @Column(name = "blocked_user_id", columnDefinition = "uuid", nullable = false)
    val blockedUserId: UUID,
) : Serializable

/** DB `user_blocks`（V23 / #215）。 */
@Entity
@Table(name = "user_blocks")
class UserBlockEntity(
    @EmbeddedId
    var id: UserBlockId,
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant,
) {
    fun toDomain(): UserBlock =
        UserBlock(
            blockerId = User.Id.of(id.blockerUserId),
            blockedId = User.Id.of(id.blockedUserId),
            createdAt = createdAt,
        )

    companion object {
        fun fromDomain(block: UserBlock): UserBlockEntity =
            UserBlockEntity(
                id = UserBlockId(blockerUserId = block.blockerId.value, blockedUserId = block.blockedId.value),
                createdAt = block.createdAt,
            )
    }
}

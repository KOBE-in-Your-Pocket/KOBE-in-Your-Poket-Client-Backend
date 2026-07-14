package com.kobeinyourpocket.backend.infrastructure.persistence.user

import com.kobeinyourpocket.backend.domain.user.model.User
import com.kobeinyourpocket.backend.domain.user.vo.UserIcon
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/** DB `users`。Supabase Auth ユーザーに紐づくプロフィール。 */
@Entity
@Table(name = "users")
class UserEntity(
    @Id
    @Column(name = "id", columnDefinition = "uuid", updatable = false, nullable = false)
    var id: UUID,
    @Column(name = "name", nullable = false, length = 100)
    var name: String,
    @Column(name = "icon_url", nullable = false)
    var iconUrl: String = "",
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,
) {
    fun toDomain(): User =
        User(
            id = User.Id.of(id),
            name = name,
            icon = iconUrl.ifEmpty { null }?.let(UserIcon::of),
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    companion object {
        fun fromDomain(user: User): UserEntity =
            UserEntity(
                id = user.id.value,
                name = user.name,
                iconUrl = user.icon?.url.orEmpty(),
                createdAt = user.createdAt,
                updatedAt = user.updatedAt,
            )
    }
}

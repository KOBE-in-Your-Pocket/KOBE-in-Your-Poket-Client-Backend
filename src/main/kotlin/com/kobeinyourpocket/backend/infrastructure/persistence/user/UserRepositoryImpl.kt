package com.kobeinyourpocket.backend.infrastructure.persistence.user

import com.kobeinyourpocket.backend.domain.user.model.User
import com.kobeinyourpocket.backend.domain.user.repository.UserRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

/** [UserRepository] port の outbound adapter。 */
@Repository
class UserRepositoryImpl(
    private val userJpa: UserJpaRepository,
) : UserRepository {
    @Transactional
    override fun save(user: User): User {
        userJpa.save(UserEntity.fromDomain(user))
        return user
    }

    override fun findById(id: User.Id): User? = userJpa.findById(id.value).map { it.toDomain() }.orElse(null)
}

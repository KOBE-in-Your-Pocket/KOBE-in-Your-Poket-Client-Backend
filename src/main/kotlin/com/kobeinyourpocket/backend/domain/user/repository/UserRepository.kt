package com.kobeinyourpocket.backend.domain.user.repository

import com.kobeinyourpocket.backend.domain.user.model.User

/** [リポジトリ] write port（command）。プロフィールの永続化。 */
interface UserRepository {
    fun save(user: User): User

    fun findById(id: User.Id): User?

    fun deleteById(id: User.Id)
}

package com.kobeinyourpocket.backend.application.user.command

import com.kobeinyourpocket.backend.application.user.auth.AuthGateway
import com.kobeinyourpocket.backend.application.user.auth.AuthSession
import com.kobeinyourpocket.backend.domain.user.model.PublicUser
import com.kobeinyourpocket.backend.domain.user.model.User
import com.kobeinyourpocket.backend.domain.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * メール+パスワードでサインアップし、Auth ユーザーと同期してプロフィール行を作る（#89-b / #88）。
 */
@Service
class SignUpService(
    private val authGateway: AuthGateway,
    private val userRepository: UserRepository,
) {
    @Transactional
    fun execute(
        email: String,
        password: String,
        name: String,
    ): AuthCommandResult {
        val session = authGateway.signUp(email = email.trim(), password = password)
        val existing = userRepository.findById(session.userId)
        val user =
            existing
                ?: User.create(id = session.userId, name = name.trim()).also { userRepository.save(it) }
        return AuthCommandResult(session = session, user = user.toPublicUser())
    }
}

/** ログイン（プロフィールが無ければ PublicUser は null）。 */
@Service
class SignInService(
    private val authGateway: AuthGateway,
    private val userRepository: UserRepository,
) {
    fun execute(
        email: String,
        password: String,
    ): AuthCommandResult {
        val session = authGateway.signInWithPassword(email = email.trim(), password = password)
        val user = userRepository.findById(session.userId)?.toPublicUser()
        return AuthCommandResult(session = session, user = user)
    }
}

@Service
class RefreshSessionService(
    private val authGateway: AuthGateway,
    private val userRepository: UserRepository,
) {
    fun execute(refreshToken: String): AuthCommandResult {
        val session = authGateway.refresh(refreshToken = refreshToken)
        val user = userRepository.findById(session.userId)?.toPublicUser()
        return AuthCommandResult(session = session, user = user)
    }
}

@Service
class SignOutService(
    private val authGateway: AuthGateway,
) {
    fun execute(accessToken: String) {
        authGateway.signOut(accessToken = accessToken)
    }
}

data class AuthCommandResult(
    val session: AuthSession,
    val user: PublicUser?,
)

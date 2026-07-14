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
 *
 * [authGateway.signUp] は外部 HTTP 呼び出しのため @Transactional 境界外で実行する。
 * DB コネクションを GoTrue の応答待ち中に確保し続けるとプール枯渇につながるため、
 * プロフィールの永続化だけを別メソッドで @Transactional にする。
 */
@Service
class SignUpService(
    private val authGateway: AuthGateway,
    private val userRepository: UserRepository,
) {
    fun execute(
        email: String,
        password: String,
        name: String,
    ): AuthCommandResult {
        val session = authGateway.signUp(email = email.trim(), password = password)
        val user = saveProfileIfAbsent(session.userId, name.trim())
        return AuthCommandResult(session = session, user = user.toPublicUser())
    }

    @Transactional
    fun saveProfileIfAbsent(
        userId: User.Id,
        name: String,
    ): User =
        userRepository.findById(userId)
            ?: User.create(id = userId, name = name).also { userRepository.save(it) }
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

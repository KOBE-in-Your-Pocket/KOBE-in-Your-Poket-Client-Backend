package com.kobeinyourpocket.backend.application.user.command

import com.kobeinyourpocket.backend.application.user.auth.AuthGateway
import com.kobeinyourpocket.backend.application.user.auth.AuthSession
import com.kobeinyourpocket.backend.domain.user.model.PublicUser
import com.kobeinyourpocket.backend.domain.user.model.User
import com.kobeinyourpocket.backend.domain.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Auth ユーザー ID に対応するプロフィール行を確保する（#89-b / #88）。
 *
 * [SignUpService] / [SignInService] からプロキシ経由で呼ばれる別 Bean。
 * 同一クラス内の自己呼び出しでは Spring AOP の `@Transactional` が効かないため切り出している。
 * [saveIfAbsent] は冪等（既存行があれば再利用）なので、Auth 作成後の失敗後も
 * 同じ [User.Id] で再実行すればプロフィールに収束できる。
 */
@Service
class EnsureUserProfileService(
    private val userRepository: UserRepository,
) {
    @Transactional
    fun saveIfAbsent(
        userId: User.Id,
        name: String,
    ): User =
        userRepository.findById(userId)
            ?: User.create(id = userId, name = name).also { userRepository.save(it) }
}

/**
 * 一時的な永続化失敗向けの有限リトライ。ドメイン不変条件違反 ([IllegalArgumentException]) は即失敗。
 * 各試行は [EnsureUserProfileService] のプロキシ経由なので `@Transactional` が効く。
 */
internal fun ensureUserProfileResilient(
    ensureUserProfileService: EnsureUserProfileService,
    userId: User.Id,
    name: String,
    maxAttempts: Int = PROFILE_SAVE_MAX_ATTEMPTS,
): User {
    require(maxAttempts >= 1) { "maxAttempts must be at least 1" }
    var lastFailure: RuntimeException? = null
    repeat(maxAttempts) {
        try {
            return ensureUserProfileService.saveIfAbsent(userId, name)
        } catch (ex: IllegalArgumentException) {
            throw ex
        } catch (ex: RuntimeException) {
            lastFailure = ex
        }
    }
    throw checkNotNull(lastFailure) { "profile save failed without exception" }
}

internal const val PROFILE_SAVE_MAX_ATTEMPTS = 3

/**
 * メール+パスワードでサインアップし、Auth ユーザーと同期してプロフィール行を作る（#89-b / #88）。
 *
 * [authGateway.signUp] は外部 HTTP 呼び出しのためトランザクション境界外で実行する。
 * Auth 成功後のプロフィール保存は有限リトライする。それでも失敗した場合は、
 * 同一 Auth ユーザーで [SignInService] が冪等修復できる（再 signup 不要）。
 * Auth ユーザー削除による補償は service_role Admin が必要なため本フローでは行わない。
 */
@Service
class SignUpService(
    private val authGateway: AuthGateway,
    private val ensureUserProfileService: EnsureUserProfileService,
) {
    fun execute(
        email: String,
        password: String,
        name: String,
    ): AuthCommandResult {
        val session = authGateway.signUp(email = email.trim(), password = password)
        val user =
            ensureUserProfileResilient(
                ensureUserProfileService = ensureUserProfileService,
                userId = session.userId,
                name = name.trim(),
            )
        return AuthCommandResult(session = session, user = user.toPublicUser())
    }
}

/**
 * ログイン。
 *
 * Auth は存在するがプロフィール行が欠ける（signup 時の保存失敗など）場合は、
 * [session.userId] で冪等にプロフィールを補完する（再 signup 不要）。
 */
@Service
class SignInService(
    private val authGateway: AuthGateway,
    private val ensureUserProfileService: EnsureUserProfileService,
) {
    fun execute(
        email: String,
        password: String,
    ): AuthCommandResult {
        val trimmedEmail = email.trim()
        val session = authGateway.signInWithPassword(email = trimmedEmail, password = password)
        val fallbackName = displayNameFromEmail(trimmedEmail)
        val user =
            ensureUserProfileResilient(
                ensureUserProfileService = ensureUserProfileService,
                userId = session.userId,
                name = fallbackName,
            )
        return AuthCommandResult(session = session, user = user.toPublicUser())
    }
}

/** プロフィール補完時の表示名。既存行があれば [EnsureUserProfileService] がそちらを優先する。 */
internal fun displayNameFromEmail(email: String): String {
    val local = email.substringBefore('@').trim()
    return local.ifBlank { "user" }
}

@Service
class RefreshSessionService(
    private val authGateway: AuthGateway,
    private val userRepository: UserRepository,
) {
    fun execute(refreshToken: String): AuthCommandResult {
        // refresh 応答には表示名が無いため、ここではプロフィール新規作成しない。
        // 欠損の修復入口は signup（リトライ）と login（冪等補完）。
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

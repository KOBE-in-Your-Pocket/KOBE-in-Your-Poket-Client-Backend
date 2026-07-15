package com.kobeinyourpocket.backend.application.user.command

import com.kobeinyourpocket.backend.application.user.auth.AuthGateway
import com.kobeinyourpocket.backend.application.user.auth.AuthSession
import com.kobeinyourpocket.backend.domain.user.model.PublicUser
import com.kobeinyourpocket.backend.domain.user.model.User
import com.kobeinyourpocket.backend.domain.user.repository.UserRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/**
 * Auth ユーザー ID に対応するプロフィール行を確保する（#89-b / #88）。
 *
 * [SignUpService] / [SignInService] からプロキシ経由で呼ばれる別 Bean。
 * 同一クラス内の自己呼び出しでは Spring AOP の `@Transactional` が効かないため切り出している。
 * [saveIfAbsent] は冪等（既存行があれば再利用）。同時 insert で PK 重複した場合も
 * 既存行を再読込して返す。
 */
@Service
class EnsureUserProfileService(
    private val userRepository: UserRepository,
    private val userProfileInserter: UserProfileInserter,
) {
    fun saveIfAbsent(
        userId: User.Id,
        name: String,
    ): User {
        userRepository.findById(userId)?.let { return it }
        return try {
            userProfileInserter.insert(userId, name)
        } catch (ex: DataIntegrityViolationException) {
            // 同時実行で先勝ち insert 済み。既存行を返して冪等に収束させる。
            // insert は REQUIRES_NEW のため、こちらの再読込はロールバックの影響を受けない。
            userRepository.findById(userId) ?: throw ex
        }
    }
}

/**
 * プロフィール新規 insert 専用。
 * [Propagation.REQUIRES_NEW] で包み、PK 重複時に外側の再読込を汚さない。
 */
@Service
class UserProfileInserter(
    private val userRepository: UserRepository,
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun insert(
        userId: User.Id,
        name: String,
    ): User = User.create(id = userId, name = name).also { userRepository.save(it) }
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

/**
 * SSO プロバイダの ID トークンでサインインする（#89-c）。
 *
 * GoTrue の `grant_type=id_token` は初回ログイン時に Auth ユーザーを自動作成するため、
 * signup / login の区別なく本サービス 1 本で済む。プロフィール行は [SignInService] と
 * 同じく冪等に補完する。表示名はプロバイダの表示名 → email ローカル部 → "user" の順。
 */
@Service
class SignInWithIdTokenService(
    private val authGateway: AuthGateway,
    private val ensureUserProfileService: EnsureUserProfileService,
) {
    fun execute(
        provider: String,
        idToken: String,
        accessToken: String? = null,
        nonce: String? = null,
    ): AuthCommandResult {
        val session =
            authGateway.signInWithIdToken(
                provider = provider,
                idToken = idToken,
                accessToken = accessToken,
                nonce = nonce,
            )
        val name = displayNameForSso(displayName = session.displayName, email = session.email)
        val user =
            ensureUserProfileResilient(
                ensureUserProfileService = ensureUserProfileService,
                userId = session.userId,
                name = name,
            )
        return AuthCommandResult(session = session, user = user.toPublicUser())
    }
}

/**
 * SSO ログイン時の表示名。プロバイダの表示名を優先し、[User.MAX_NAME_LENGTH] を超える分は
 * ドメイン不変条件違反（即失敗）にせず切り詰める。
 */
internal fun displayNameForSso(
    displayName: String?,
    email: String?,
): String {
    val name = displayName?.trim().orEmpty().ifBlank { displayNameFromEmail(email.orEmpty()) }
    return name.take(User.MAX_NAME_LENGTH)
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

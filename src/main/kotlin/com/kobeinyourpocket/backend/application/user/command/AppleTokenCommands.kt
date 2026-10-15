package com.kobeinyourpocket.backend.application.user.command

import com.kobeinyourpocket.backend.application.user.auth.AppleTokenGateway
import com.kobeinyourpocket.backend.domain.user.model.User
import com.kobeinyourpocket.backend.domain.user.repository.AppleRefreshTokenRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Apple の ID トークンでサインインし、`authorizationCode` があれば refresh token に交換して保存する（#192 / #213）。
 *
 * サインイン自体は [SignInWithIdTokenService] に任せる。refresh token は退会時の失効にだけ使う。
 *
 * 交換・保存の失敗ではサインインを失敗させない。失効できないユーザーが残るだけで、
 * サインインを止めるほどではないため。`authorizationCode` は 5 分・1 回限りで送り直せないので、
 * 失敗はログに残すだけにする（次回サインイン時に改めて保存される）。
 */
@Service
class SignInWithAppleService(
    private val signInWithIdTokenService: SignInWithIdTokenService,
    private val appleTokenGateway: AppleTokenGateway,
    private val appleRefreshTokenRepository: AppleRefreshTokenRepository,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun execute(
        idToken: String,
        accessToken: String? = null,
        nonce: String? = null,
        name: String? = null,
        authorizationCode: String? = null,
    ): AuthCommandResult {
        val result =
            signInWithIdTokenService.execute(
                provider = "apple",
                idToken = idToken,
                accessToken = accessToken,
                nonce = nonce,
                name = name,
            )
        authorizationCode
            ?.takeIf { it.isNotBlank() }
            ?.let { saveRefreshToken(userId = result.session.userId, idToken = idToken, authorizationCode = it) }
        return result
    }

    private fun saveRefreshToken(
        userId: User.Id,
        idToken: String,
        authorizationCode: String,
    ) {
        if (!appleTokenGateway.isConfigured) {
            logger.debug("Apple の秘密鍵が未設定のため refresh token を保存しない: userId={}", userId)
            return
        }
        try {
            val grant = appleTokenGateway.exchangeAuthorizationCode(authorizationCode)
            // 別の Apple ID のコードを紐づけない（退会時に他人の連携を解除しないため）。
            val expectedSubject = appleTokenGateway.subjectOf(idToken)
            if (grant.subject == null || grant.subject != expectedSubject) {
                logger.warn("Apple の authorizationCode が ID トークンと別の Apple ID のため保存しない: userId={}", userId)
                return
            }
            appleRefreshTokenRepository.save(userId, grant.refreshToken)
        } catch (ex: RuntimeException) {
            logger.warn("Apple の refresh token を保存できなかった: userId={}, reason={}", userId, ex.message)
        }
    }
}

/**
 * 退会時に Apple の refresh token を失効させ、保存していた値を消す（#213）。
 *
 * Apple 側でアプリと Apple ID の連携が解除される（App Store 審査の要件）。
 * 失効に失敗しても退会は止めない。Apple 側の一時的な失敗で退会できなくなるのを避けるため。
 * 失敗しても再試行はせず保存値も消す（プロフィール行の削除で外部キーにより消えるため残せない）。
 * 連携が残ったユーザーを後から追えるよう、失敗は例外ごと error で記録する。
 * 保存値が無い（この対応より前にサインインした・Apple 以外のユーザー）場合は何もしない。
 */
@Service
class RevokeAppleTokenService(
    private val appleTokenGateway: AppleTokenGateway,
    private val appleRefreshTokenRepository: AppleRefreshTokenRepository,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun execute(userId: User.Id) {
        val refreshToken = appleRefreshTokenRepository.findByUserId(userId) ?: return
        try {
            appleTokenGateway.revokeRefreshToken(refreshToken)
        } catch (ex: RuntimeException) {
            // 例外のメッセージ・原因に token は含まれない（AppleTokenException / Apple のエラー本文のみ）。
            logger.error("Apple のトークンを失効できなかった（連携が残っている）: userId={}", userId, ex)
        }
        appleRefreshTokenRepository.deleteByUserId(userId)
    }
}

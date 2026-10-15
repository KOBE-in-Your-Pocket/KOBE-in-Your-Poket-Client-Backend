package com.kobeinyourpocket.backend.application.user.auth

/**
 * Sign in with Apple の REST API（`appleid.apple.com` の `/auth/token`・`/auth/revoke`）への outbound port（#213）。
 *
 * 退会時に Apple のトークンを失効させる（App Store 審査の要件）ために使う。
 * サインイン自体は [AuthGateway.signInWithIdToken]（GoTrue）が行い、本 port は関与しない。
 */
interface AppleTokenGateway {
    /** Team ID・Key ID・秘密鍵が揃っていて、Apple の API を呼べるか。 */
    val isConfigured: Boolean

    /**
     * Client から受け取った `authorizationCode` を Apple の `/auth/token` で交換する。
     * コードは発行から 5 分以内・1 回限り有効。
     *
     * @throws AppleTokenException 交換に失敗した場合（未設定・Apple のエラー・通信失敗）
     */
    fun exchangeAuthorizationCode(authorizationCode: String): AppleTokenGrant

    /**
     * refresh token を Apple の `/auth/revoke` で失効させる。
     * Apple 側でアプリと Apple ID の連携が解除される。
     *
     * @throws AppleTokenException 失効に失敗した場合
     */
    fun revokeRefreshToken(refreshToken: String)

    /** Apple の ID トークンから `sub`（Apple のユーザー識別子）を取り出す。署名は検証しない。 */
    fun subjectOf(idToken: String): String?
}

/** `authorizationCode` の交換結果。 */
data class AppleTokenGrant(
    val refreshToken: String,
    /** 交換で得た ID トークンの `sub`。サインインに使った ID トークンと同じ Apple ID かの照合に使う。 */
    val subject: String?,
) {
    // refresh token をログや例外メッセージへ出さない。
    override fun toString(): String = "AppleTokenGrant(subject=$subject)"
}

/** Apple の REST API 呼び出しの失敗。メッセージに token を含めない。 */
class AppleTokenException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

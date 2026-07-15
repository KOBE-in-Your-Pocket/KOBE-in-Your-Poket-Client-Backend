package com.kobeinyourpocket.backend.application.user.auth

import com.kobeinyourpocket.backend.domain.user.model.User

/**
 * Supabase Auth（GoTrue）への outbound port。
 *
 * パスワード保管・JWT 発行は Gateway 先（Supabase）が行い、backend は中継のみ。
 */
interface AuthGateway {
    fun signUp(
        email: String,
        password: String,
    ): AuthSession

    fun signInWithPassword(
        email: String,
        password: String,
    ): AuthSession

    /**
     * SSO プロバイダ発行の ID トークンでサインイン（#89-c）。
     * GoTrue の `grant_type=id_token` に中継する。初回はユーザーが自動作成される。
     *
     * @param provider GoTrue が受け付けるプロバイダ名（例: "google"）
     * @param accessToken プロバイダのアクセストークン（Google では通常不要）
     * @param nonce ID トークン取得時に使った nonce（トークンに含まれる場合は必須）
     */
    fun signInWithIdToken(
        provider: String,
        idToken: String,
        accessToken: String? = null,
        nonce: String? = null,
    ): AuthSession

    fun refresh(refreshToken: String): AuthSession

    fun signOut(accessToken: String)
}

/** Auth 成功後に Client へ返すセッション（トークンは Supabase 発行分）。 */
data class AuthSession(
    val userId: User.Id,
    val accessToken: String?,
    val refreshToken: String?,
    val expiresIn: Long?,
    val tokenType: String?,
    /** GoTrue レスポンスの user.email。SSO 初回ログイン時の表示名フォールバックに使う。 */
    val email: String? = null,
    /** GoTrue レスポンスの user_metadata 由来の表示名（Google の full_name 等）。 */
    val displayName: String? = null,
)

/** GoTrue 呼び出し失敗。HTTP ステータスを呼び出し側へ伝える。 */
class AuthGatewayException(
    val status: Int,
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

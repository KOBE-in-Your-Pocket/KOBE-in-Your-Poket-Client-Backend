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

    fun refresh(refreshToken: String): AuthSession

    fun signOut(accessToken: String)

    /**
     * Supabase Admin API でユーザーを完全削除する（service_role キーが必要）。
     *
     * 既に削除済み（404）は冪等成功として扱う。再試行で Auth と DB プロフィールの
     * 片側だけ残った状態を収束させるため。
     * 認可は呼び出し側（[com.kobeinyourpocket.backend.application.user.command.DeleteUserService]）が担保する。
     */
    fun deleteUser(userId: User.Id)
}

/** Auth 成功後に Client へ返すセッション（トークンは Supabase 発行分）。 */
data class AuthSession(
    val userId: User.Id,
    val accessToken: String?,
    val refreshToken: String?,
    val expiresIn: Long?,
    val tokenType: String?,
)

/** GoTrue 呼び出し失敗。HTTP ステータスを呼び出し側へ伝える。 */
class AuthGatewayException(
    val status: Int,
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

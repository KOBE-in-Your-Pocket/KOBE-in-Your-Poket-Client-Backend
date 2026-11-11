package com.kobeinyourpocket.backend.infrastructure.rest.user

import com.kobeinyourpocket.backend.application.user.command.AuthCommandResult
import com.kobeinyourpocket.backend.domain.user.model.PublicUser
import com.kobeinyourpocket.backend.domain.user.model.User
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class SignUpRequest(
    @field:NotBlank @field:Email
    val email: String,
    /** 下限 8 文字（#244）。Supabase 側の最小パスワード長も 8 に揃える。 */
    @field:NotBlank @field:Size(min = PASSWORD_MIN_LENGTH, max = PASSWORD_MAX_LENGTH)
    val password: String,
    @field:NotBlank @field:Size(max = User.MAX_NAME_LENGTH)
    val name: String,
) {
    companion object {
        const val PASSWORD_MIN_LENGTH = 8
        const val PASSWORD_MAX_LENGTH = 128
    }
}

/**
 * ログインには長さ制約を付けない（#244）。下限を上げる前に 6〜7 文字で登録したユーザーも
 * ログインできるようにするため。可否の判定は GoTrue に任せる。
 */
data class SignInRequest(
    @field:NotBlank @field:Email
    val email: String,
    @field:NotBlank
    val password: String,
)

data class RefreshRequest(
    @field:NotBlank
    val refreshToken: String,
)

/**
 * SSO の ID トークンサインイン（#89-c / #192 / #196）。
 * Client がネイティブ SDK で取得した Google / Apple / Facebook 等の ID トークンを backend が GoTrue へ中継する。
 */
data class IdTokenSignInRequest(
    @field:NotBlank
    val idToken: String,
    val accessToken: String? = null,
    val nonce: String? = null,
    /** Apple が初回認証時のみ返す表示名（fullName）。プロフィール新規作成時のみ使い、既存の表示名は上書きしない（#193）。 */
    @field:Size(max = User.MAX_NAME_LENGTH)
    val name: String? = null,
    /**
     * Apple の `authorizationCode`（#213）。refresh token に交換して保存し、退会時の失効に使う。
     * Apple 以外のプロバイダでは使わない。
     */
    @field:Size(max = AUTHORIZATION_CODE_MAX_LENGTH)
    val authorizationCode: String? = null,
) {
    companion object {
        const val AUTHORIZATION_CODE_MAX_LENGTH = 1024
    }
}

data class AuthSessionResponse(
    val accessToken: String?,
    val refreshToken: String?,
    val expiresIn: Long?,
    val tokenType: String?,
    val user: PublicUserResponse?,
) {
    companion object {
        fun from(result: AuthCommandResult): AuthSessionResponse =
            AuthSessionResponse(
                accessToken = result.session.accessToken,
                refreshToken = result.session.refreshToken,
                expiresIn = result.session.expiresIn,
                tokenType = result.session.tokenType,
                user = result.user?.let(PublicUserResponse::from),
            )
    }
}

data class PublicUserResponse(
    val id: String,
    val name: String,
    val iconUrl: String?,
) {
    companion object {
        fun from(user: PublicUser): PublicUserResponse =
            PublicUserResponse(
                id = user.id.value.toString(),
                name = user.name,
                iconUrl = user.iconUrl,
            )
    }
}

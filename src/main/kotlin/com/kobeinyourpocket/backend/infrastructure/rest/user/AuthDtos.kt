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
    @field:NotBlank @field:Size(min = 6, max = 128)
    val password: String,
    @field:NotBlank @field:Size(max = User.MAX_NAME_LENGTH)
    val name: String,
)

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
 * SSO の ID トークンサインイン（#89-c）。
 * Client がネイティブ SDK で取得した Google 等の ID トークンを backend が GoTrue へ中継する。
 */
data class IdTokenSignInRequest(
    @field:NotBlank
    val idToken: String,
    val accessToken: String? = null,
    val nonce: String? = null,
)

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

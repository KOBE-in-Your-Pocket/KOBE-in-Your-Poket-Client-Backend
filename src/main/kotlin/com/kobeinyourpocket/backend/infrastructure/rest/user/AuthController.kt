package com.kobeinyourpocket.backend.infrastructure.rest.user

import com.kobeinyourpocket.backend.application.user.command.RefreshSessionService
import com.kobeinyourpocket.backend.application.user.command.SignInService
import com.kobeinyourpocket.backend.application.user.command.SignOutService
import com.kobeinyourpocket.backend.application.user.command.SignUpService
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 認証プロキシ API（#89-b）。
 *
 * Client は Supabase に直接触れず、本 Controller 経由で GoTrue を呼び出す。
 */
@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val signUpService: SignUpService,
    private val signInService: SignInService,
    private val refreshSessionService: RefreshSessionService,
    private val signOutService: SignOutService,
) {
    @PostMapping("/signup")
    fun signUp(
        @Valid @RequestBody request: SignUpRequest,
    ): ResponseEntity<AuthSessionResponse> {
        val result =
            signUpService.execute(
                email = request.email,
                password = request.password,
                name = request.name,
            )
        return ResponseEntity.status(HttpStatus.CREATED).body(AuthSessionResponse.from(result))
    }

    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: SignInRequest,
    ): AuthSessionResponse =
        AuthSessionResponse.from(
            signInService.execute(
                email = request.email,
                password = request.password,
            ),
        )

    @PostMapping("/refresh")
    fun refresh(
        @Valid @RequestBody request: RefreshRequest,
    ): AuthSessionResponse = AuthSessionResponse.from(refreshSessionService.execute(request.refreshToken))

    @PostMapping("/logout")
    fun logout(
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) authorization: String?,
    ): ResponseEntity<Void> {
        val token =
            bearerToken(authorization)
                ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        signOutService.execute(token)
        return ResponseEntity.noContent().build()
    }

    private fun bearerToken(authorization: String?): String? {
        if (authorization.isNullOrBlank()) return null
        val prefix = "Bearer "
        if (!authorization.startsWith(prefix, ignoreCase = true)) return null
        return authorization.substring(prefix.length).trim().ifBlank { null }
    }
}

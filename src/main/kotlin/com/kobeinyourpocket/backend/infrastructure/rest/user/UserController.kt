package com.kobeinyourpocket.backend.infrastructure.rest.user

import com.kobeinyourpocket.backend.application.user.query.GetMeService
import com.kobeinyourpocket.backend.domain.user.model.User
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * ユーザー API（#91）。
 *
 * `GET /me` は「閲覧系はオープン」（U-2）の例外として認証必須（U-1）。
 * SecurityConfig の URL ルールではなくメソッドセキュリティで守るため、
 * #90 の認可ポリシー変更と独立に成立する（未認証は EntryPoint 経由で 401）。
 */
@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val getMeService: GetMeService,
) {
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    fun me(
        @AuthenticationPrincipal jwt: Jwt,
    ): PublicUserResponse {
        val subject = requireNotNull(jwt.subject) { "JWT subject is missing" }
        return PublicUserResponse.from(getMeService.execute(User.Id.of(subject)))
    }
}

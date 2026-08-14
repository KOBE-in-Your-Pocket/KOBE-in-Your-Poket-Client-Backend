package com.kobeinyourpocket.backend.infrastructure.rest.user

import com.kobeinyourpocket.backend.application.user.query.GetMeService
import com.kobeinyourpocket.backend.application.user.query.ListUsersService
import com.kobeinyourpocket.backend.domain.user.model.User
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * ユーザー API（#91 / #151）。
 *
 * `GET /me` は「閲覧系はオープン」（U-2）の例外として認証必須（U-1）。
 * SecurityConfig の URL ルールではなくメソッドセキュリティで守るため、
 * #90 の認可ポリシー変更と独立に成立する（未認証は EntryPoint 経由で 401）。
 * 一覧も同じ理由でメソッドセキュリティ側に置く。
 */
@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val getMeService: GetMeService,
    private val listUsersService: ListUsersService,
) {
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    fun me(
        @AuthenticationPrincipal jwt: Jwt,
    ): PublicUserResponse {
        val subject = requireNotNull(jwt.subject) { "JWT subject is missing" }
        return PublicUserResponse.from(getMeService.execute(User.Id.of(subject)))
    }

    /**
     * 運営向けユーザー一覧（#151）。管理画面のユーザー管理・削除対象の選択に使う。
     *
     * 閲覧は運営業務（要件 C-15）なので operator にも開く。
     * 削除（`DELETE /api/v1/auth/users/{userId}`）だけが admin 限定（A-1）。
     * ロール階層で ADMIN > OPERATOR のため admin も通る。
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    fun list(
        @RequestParam(name = "page", required = false) page: Int?,
        @RequestParam(name = "size", required = false) size: Int?,
    ): UserListResponse = UserListResponse.from(listUsersService.listUsers(page = page, size = size))
}

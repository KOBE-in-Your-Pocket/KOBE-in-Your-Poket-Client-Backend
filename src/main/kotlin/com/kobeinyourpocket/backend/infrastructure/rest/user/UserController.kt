package com.kobeinyourpocket.backend.infrastructure.rest.user

import com.kobeinyourpocket.backend.application.user.command.DeleteUserService
import com.kobeinyourpocket.backend.application.user.command.UpdateOwnIconService
import com.kobeinyourpocket.backend.application.user.command.UpdateOwnProfileService
import com.kobeinyourpocket.backend.application.user.query.GetMeService
import com.kobeinyourpocket.backend.application.user.query.ListUsersService
import com.kobeinyourpocket.backend.domain.user.model.User
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

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
    private val updateOwnProfileService: UpdateOwnProfileService,
    private val updateOwnIconService: UpdateOwnIconService,
    private val deleteUserService: DeleteUserService,
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
     * 本人によるプロフィール更新（#179）。表示名とアイコンを部分更新する。
     *
     * 更新対象は JWT の subject から決まるため、他人の id を指定する余地が無い。
     * アイコンの 3 状態（変更しない / 未設定に戻す / 差し替える）の表し方は [UpdateMeRequest] を参照。
     *
     * 表示名が空や長すぎる場合はドメインの不変条件違反として 400
     * （`IllegalArgumentException` → [com.kobeinyourpocket.backend.infrastructure.rest.common.GlobalExceptionHandler]）。
     */
    @PatchMapping("/me")
    @PreAuthorize("isAuthenticated()")
    fun updateMe(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: UpdateMeRequest,
    ): PublicUserResponse {
        val subject = requireNotNull(jwt.subject) { "JWT subject is missing" }
        val updated =
            updateOwnProfileService.execute(
                userId = User.Id.of(subject),
                name = request.name,
                icon = request.toIconUpdate(),
            )
        return PublicUserResponse.from(updated.toPublicUser())
    }

    /**
     * 本人によるアイコン画像の差し替え（#184）。写真ライブラリで選んだ画像をそのまま受ける。
     *
     * アップロードと設定を 1 回で終える。運営向けの `POST /api/v1/media/uploads` と分けているのは、
     * 一般利用者に開くぶんサイズ上限を小さくし、EXIF を落とす再エンコードを必ず通すため。
     * 返すのは更新後のプロフィールで、Client は `iconUrl` をそのまま表示に使える。
     *
     * 大きすぎる・画像として読めない場合は 400、5MB（multipart 上限）を超える場合は 413。
     */
    @PostMapping("/me/icon", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @PreAuthorize("isAuthenticated()")
    fun updateMyIcon(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestParam("file") file: MultipartFile,
    ): PublicUserResponse {
        val subject = requireNotNull(jwt.subject) { "JWT subject is missing" }
        val updated = updateOwnIconService.execute(userId = User.Id.of(subject), bytes = file.bytes)
        return PublicUserResponse.from(updated.toPublicUser())
    }

    /**
     * 本人によるアカウント削除（退会）。
     *
     * App Store Guideline 5.1.1(v) は、アカウント作成を提供するアプリに対して
     * アプリ内からの自己削除を求める。運営による削除（`DELETE /api/v1/auth/users/{userId}`、
     * ADMIN 限定）とは別に、利用者が自分のトークンだけで実行できる導線として用意する。
     *
     * 削除対象は JWT の subject から決まるため、他人の id を指定する余地が無い。
     * 冪等ではなく、プロフィール行が既に無い場合は 404（[GlobalExceptionHandler]）。
     * 二重実行は UI 側で退会後にサインアウトすることで避ける。
     */
    @DeleteMapping("/me")
    @PreAuthorize("isAuthenticated()")
    fun deleteMe(
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<Void> {
        val subject = requireNotNull(jwt.subject) { "JWT subject is missing" }
        deleteUserService.execute(User.Id.of(subject))
        return ResponseEntity.noContent().build()
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

package com.kobeinyourpocket.backend.infrastructure.rest.user

import com.kobeinyourpocket.backend.application.user.command.BlockUserService
import com.kobeinyourpocket.backend.application.user.command.UnblockUserService
import com.kobeinyourpocket.backend.application.user.query.ListBlockedUsersService
import com.kobeinyourpocket.backend.domain.user.model.User
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 利用者によるブロック API（#215）。App Store Guideline 1.2 のレビュー解禁条件。
 *
 * ブロックする側は JWT の `sub` から決まり、他人のブロックを操作する余地が無いため一般ロールで許可する。
 * ブロック・解除はどちらも冪等なので、Client は「今の状態にする」つもりで何度呼んでもよい。
 */
@RestController
@RequestMapping("/api/v1/users/me/blocks")
class UserBlockController(
    private val blockUserService: BlockUserService,
    private val unblockUserService: UnblockUserService,
    private val listBlockedUsersService: ListBlockedUsersService,
) {
    /** ブロック中のユーザー一覧（ブロックした日時の新しい順）。設定画面の解除 UI に使う。 */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    fun list(
        @AuthenticationPrincipal jwt: Jwt,
    ): List<BlockedUserResponse> = listBlockedUsersService.execute(requesterId(jwt)).map(BlockedUserResponse::from)

    /**
     * ユーザーをブロックする。
     *
     * - 204: ブロックした（すでにブロック済みでも 204）
     * - 400: 自分自身 / userId の形式が不正
     * - 404: ユーザーが存在しない
     */
    @PutMapping("/{userId}")
    @PreAuthorize("isAuthenticated()")
    fun block(
        @PathVariable userId: String,
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<Void> {
        blockUserService.execute(blockerId = requesterId(jwt), blockedId = User.Id.of(userId))
        return ResponseEntity.noContent().build()
    }

    /** ブロックを解除する。ブロックしていない相手でも 204（冪等）。userId の形式が不正なら 400。 */
    @DeleteMapping("/{userId}")
    @PreAuthorize("isAuthenticated()")
    fun unblock(
        @PathVariable userId: String,
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<Void> {
        unblockUserService.execute(blockerId = requesterId(jwt), blockedId = User.Id.of(userId))
        return ResponseEntity.noContent().build()
    }

    private fun requesterId(jwt: Jwt): User.Id = User.Id.of(requireNotNull(jwt.subject) { "JWT subject is missing" })
}

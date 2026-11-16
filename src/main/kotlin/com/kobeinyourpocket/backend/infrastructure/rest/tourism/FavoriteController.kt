package com.kobeinyourpocket.backend.infrastructure.rest.tourism

import com.kobeinyourpocket.backend.application.tourism.command.AddFavoriteService
import com.kobeinyourpocket.backend.application.tourism.command.RemoveFavoriteService
import com.kobeinyourpocket.backend.domain.tourism.favorite.vo.FavoriteOwnerId
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 観光スポットのお気に入り API（#236 / PBI #227。仕様は docs/favorite.md）。
 *
 * 本人は JWT の `sub` から決まり、他人のお気に入りを操作する余地が無いため一般ロールで許可する。
 * 追加・解除はどちらも冪等なので、Client は「今の状態にする」つもりで何度呼んでもよい。
 */
@RestController
@RequestMapping("/api/v1/users/me/favorites")
class FavoriteController(
    private val addFavoriteService: AddFavoriteService,
    private val removeFavoriteService: RemoveFavoriteService,
) {
    /**
     * お気に入りに追加する。
     *
     * - 204: 追加した（登録済みでも 204）
     * - 400: spotId が長すぎる
     * - 404: スポットが存在しない
     */
    @PutMapping("/{spotId}")
    @PreAuthorize("isAuthenticated()")
    fun add(
        @PathVariable spotId: String,
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<Void> {
        addFavoriteService.execute(ownerId = ownerId(jwt), spotId = SpotId.of(spotId))
        return ResponseEntity.noContent().build()
    }

    /** お気に入りを解除する。登録していないスポットでも 204（冪等）。spotId が長すぎれば 400。 */
    @DeleteMapping("/{spotId}")
    @PreAuthorize("isAuthenticated()")
    fun remove(
        @PathVariable spotId: String,
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<Void> {
        removeFavoriteService.execute(ownerId = ownerId(jwt), spotId = SpotId.of(spotId))
        return ResponseEntity.noContent().build()
    }

    private fun ownerId(jwt: Jwt): FavoriteOwnerId = FavoriteOwnerId.of(requireNotNull(jwt.subject) { "JWT subject is missing" })
}

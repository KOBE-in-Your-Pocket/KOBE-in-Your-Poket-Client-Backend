package com.kobeinyourpocket.backend.infrastructure.rest.tourism

import com.kobeinyourpocket.backend.application.tourism.command.RecordSpotViewService
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 観光スポットの閲覧を数える API（#240 / PBI #228。仕様は docs/spot-view.md）。
 *
 * 未ログインでも呼べる（SecurityConfig で公開）。誰が閲覧したかは記録しない。
 *
 * 同じ IP から同じスポットへの閲覧は 10 分に 1 回だけ数え、同じ IP から 1 分に 60 回を超えると 429 にする（#260）。
 * 429 は [com.kobeinyourpocket.backend.infrastructure.ratelimit.SpotViewIpRateLimitFilter] が返す。
 */
@RestController
@RequestMapping("/api/v1/tourism/spots/{spotId}/views")
class SpotViewController(
    private val recordSpotViewService: RecordSpotViewService,
) {
    /**
     * 閲覧を 1 回数える。
     *
     * - 204: 数えた。または同じ IP から 10 分以内の重複としてまとめた（数えないだけでエラーにしない）
     * - 400: spotId が長すぎる
     * - 404: スポットが存在しない
     *
     * 利用者 IP は `request.remoteAddr`。本番は Caddy の `X-Forwarded-For` を
     * Tomcat（`server.forward-headers-strategy: native`）が解釈した実 IP になる。
     */
    @PostMapping
    fun record(
        @PathVariable spotId: String,
        request: HttpServletRequest,
    ): ResponseEntity<Void> {
        recordSpotViewService.execute(SpotId.of(spotId), viewerKey = request.remoteAddr)
        return ResponseEntity.noContent().build()
    }
}

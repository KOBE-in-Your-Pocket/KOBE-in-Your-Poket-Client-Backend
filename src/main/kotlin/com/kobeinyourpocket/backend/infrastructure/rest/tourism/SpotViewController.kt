package com.kobeinyourpocket.backend.infrastructure.rest.tourism

import com.kobeinyourpocket.backend.application.tourism.command.RecordSpotViewService
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 観光スポットの閲覧を数える API（#240 / PBI #228。仕様は docs/spot-view.md）。
 *
 * 未ログインでも呼べる（SecurityConfig で公開）。誰が閲覧したかは記録しない。
 */
@RestController
@RequestMapping("/api/v1/tourism/spots/{spotId}/views")
class SpotViewController(
    private val recordSpotViewService: RecordSpotViewService,
) {
    /**
     * 閲覧を 1 回数える。
     *
     * - 204: 数えた
     * - 400: spotId が長すぎる
     * - 404: スポットが存在しない
     */
    @PostMapping
    fun record(
        @PathVariable spotId: String,
    ): ResponseEntity<Void> {
        recordSpotViewService.execute(SpotId.of(spotId))
        return ResponseEntity.noContent().build()
    }
}

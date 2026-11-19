package com.kobeinyourpocket.backend.domain.tourism.view.model

import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId
import java.time.Instant

/**
 * [エンティティ] 観光スポットの閲覧 1 回（#239 / PBI #228）。
 *
 * Client で観光スポットのカードから詳細画面に遷移したときに 1 件記録する（Client#777）。
 * 未ログインでも数えるため、誰が閲覧したかは持たない。閲覧数は記録の件数で表す。
 */
data class SpotView(
    val spotId: SpotId,
    val viewedAt: Instant,
) {
    companion object {
        fun create(
            spotId: SpotId,
            viewedAt: Instant = Instant.now(),
        ): SpotView = SpotView(spotId = spotId, viewedAt = viewedAt)
    }
}

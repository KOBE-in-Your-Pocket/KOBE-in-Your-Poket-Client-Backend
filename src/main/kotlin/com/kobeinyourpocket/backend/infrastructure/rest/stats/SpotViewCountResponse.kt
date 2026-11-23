package com.kobeinyourpocket.backend.infrastructure.rest.stats

import com.kobeinyourpocket.backend.application.tourism.query.SpotViewCount

/** `GET /api/v1/stats/spot-views` のレスポンス 1 件（管理画面用 / #241）。 */
data class SpotViewCountResponse(
    val spotId: String,
    val name: String,
    val viewCount: Long,
) {
    companion object {
        fun from(view: SpotViewCount): SpotViewCountResponse =
            SpotViewCountResponse(
                spotId = view.spotId,
                name = view.name,
                viewCount = view.viewCount,
            )
    }
}

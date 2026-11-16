package com.kobeinyourpocket.backend.infrastructure.rest.tourism

import com.kobeinyourpocket.backend.application.tourism.query.FavoriteView
import java.time.Instant

/** `GET /api/v1/users/me/favorites` の 1 件（#237）。Client の `Favorite` 型（Client#789）と同じ形。 */
data class FavoriteResponse(
    val spotId: String,
    val createdAt: Instant,
) {
    companion object {
        fun from(view: FavoriteView): FavoriteResponse = FavoriteResponse(spotId = view.spotId, createdAt = view.createdAt)
    }
}

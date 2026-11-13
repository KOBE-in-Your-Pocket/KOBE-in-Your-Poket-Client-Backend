package com.kobeinyourpocket.backend.domain.tourism.favorite.model

import com.kobeinyourpocket.backend.domain.tourism.favorite.vo.FavoriteOwnerId
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId
import java.time.Instant

/**
 * [エンティティ] 利用者による観光スポットのお気に入り（#235 / PBI #227）。
 *
 * Client `Favorite`（spotId / createdAt。Client#789）に対応する。
 *
 * 同一性は [ownerId] と [spotId] の組で決まる。同じスポットを 2 回登録しても 1 件として扱う。
 */
data class Favorite(
    val ownerId: FavoriteOwnerId,
    val spotId: SpotId,
    val createdAt: Instant,
) {
    companion object {
        fun create(
            ownerId: FavoriteOwnerId,
            spotId: SpotId,
            createdAt: Instant = Instant.now(),
        ): Favorite = Favorite(ownerId = ownerId, spotId = spotId, createdAt = createdAt)
    }
}

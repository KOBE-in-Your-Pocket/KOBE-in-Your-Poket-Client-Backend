package com.kobeinyourpocket.backend.application.tourism.command

import com.kobeinyourpocket.backend.domain.tourism.favorite.repository.FavoriteRepository
import com.kobeinyourpocket.backend.domain.tourism.favorite.vo.FavoriteOwnerId
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId
import org.springframework.stereotype.Service

/**
 * お気に入りを解除するユースケース（#236 / PBI #227）。
 *
 * 登録していないスポットを指定しても成功として扱う（冪等）。スポットの存在も確かめない。
 * スポットが削除されたときはお気に入りも外部キー（V25）で消えるため、残った行を気にする必要が無い。
 */
@Service
class RemoveFavoriteService(
    private val favoriteRepository: FavoriteRepository,
) {
    fun execute(
        ownerId: FavoriteOwnerId,
        spotId: SpotId,
    ) {
        favoriteRepository.delete(ownerId = ownerId, spotId = spotId)
    }
}

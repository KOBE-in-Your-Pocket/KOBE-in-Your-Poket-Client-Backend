package com.kobeinyourpocket.backend.application.tourism.query

import com.kobeinyourpocket.backend.domain.tourism.favorite.vo.FavoriteOwnerId
import org.springframework.stereotype.Service

/** 自分のお気に入り一覧取得ユースケース（read / #237）。[FavoriteQuery] port へ委譲する。 */
@Service
class ListFavoritesService(
    private val favoriteQuery: FavoriteQuery,
) {
    fun execute(ownerId: FavoriteOwnerId): List<FavoriteView> = favoriteQuery.findByOwner(ownerId)
}

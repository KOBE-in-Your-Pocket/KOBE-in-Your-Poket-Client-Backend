package com.kobeinyourpocket.backend.application.tourism.command

import com.kobeinyourpocket.backend.application.tourism.SpotNotFoundException
import com.kobeinyourpocket.backend.domain.tourism.favorite.model.Favorite
import com.kobeinyourpocket.backend.domain.tourism.favorite.repository.FavoriteRepository
import com.kobeinyourpocket.backend.domain.tourism.favorite.vo.FavoriteOwnerId
import com.kobeinyourpocket.backend.domain.tourism.spot.repository.SpotRepository
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service

/**
 * 観光スポットをお気に入りに追加するユースケース（#236 / PBI #227）。
 *
 * スポットが存在しなければ 404（[SpotNotFoundException]）。すでに登録済みでも成功として扱う（冪等）。
 * [ownerId] は REST 層が JWT の `sub` から渡す。
 *
 * トランザクションは張らない。[FavoriteRepository.saveIfAbsent] は、同時送信による主キー違反を
 * 成功扱いにするため、トランザクションの外で呼ばれる前提になっている。
 */
@Service
class AddFavoriteService(
    private val spotRepository: SpotRepository,
    private val favoriteRepository: FavoriteRepository,
) {
    fun execute(
        ownerId: FavoriteOwnerId,
        spotId: SpotId,
    ) {
        if (!spotRepository.existsById(spotId)) throw SpotNotFoundException(spotId)
        try {
            favoriteRepository.saveIfAbsent(Favorite.create(ownerId = ownerId, spotId = spotId))
        } catch (e: DataIntegrityViolationException) {
            // 存在確認と保存の間にスポットが削除された（V25 の fk_favorites_spot で弾かれる）。
            // 500 のままにせず、事前確認と同じ 404 にそろえる（DeleteGenreService と同じ考え方）。
            throw SpotNotFoundException(spotId).apply { initCause(e) }
        }
    }
}

package com.kobeinyourpocket.backend.infrastructure.persistence.tourism

import com.kobeinyourpocket.backend.domain.tourism.favorite.model.Favorite
import com.kobeinyourpocket.backend.domain.tourism.favorite.repository.FavoriteRepository
import com.kobeinyourpocket.backend.domain.tourism.favorite.vo.FavoriteOwnerId
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

/** [FavoriteRepository] port の outbound adapter。 */
@Repository
class FavoriteRepositoryImpl(
    private val favoriteJpa: FavoriteJpaRepository,
) : FavoriteRepository {
    /**
     * 通常の重複は事前の存在確認で弾き、同時送信ですり抜けた分は主キー違反で弾く。
     *
     * 一意制約違反には、主キー違反（同時に同じ組を登録した）のほかに、スポットへの外部キー違反
     * （スポットが存在しない・登録の直前に削除された）もある。後者まで成功扱いにすると、
     * 保存されていないのに成功を返してしまう。そのため違反のあとに行があるかを確かめ直し、
     * 行があるときだけ成功とし、無ければ例外をそのまま投げる。
     *
     * このメソッド自体はトランザクションを張らない。張ると制約違反の時点で
     * トランザクションが rollback-only になり、捕まえてもコミット時に例外になるため
     * （[com.kobeinyourpocket.backend.infrastructure.persistence.user.UserBlockRepositoryImpl] と同じ）。
     */
    override fun saveIfAbsent(favorite: Favorite) {
        val entity = FavoriteEntity.fromDomain(favorite)
        if (favoriteJpa.existsById(entity.id)) return
        try {
            favoriteJpa.saveAndFlush(entity)
        } catch (e: DataIntegrityViolationException) {
            // 同時に同じスポットを登録した場合は行がある。結果は同じなので成功として扱う。
            if (favoriteJpa.existsById(entity.id)) return
            throw e
        }
    }

    @Transactional
    override fun delete(
        ownerId: FavoriteOwnerId,
        spotId: SpotId,
    ) {
        favoriteJpa.deleteById(FavoriteKey(userId = ownerId.value, spotId = spotId.value))
    }
}

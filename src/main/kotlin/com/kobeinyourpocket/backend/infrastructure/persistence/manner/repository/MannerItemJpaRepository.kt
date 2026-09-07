package com.kobeinyourpocket.backend.infrastructure.persistence.manner.repository

import com.kobeinyourpocket.backend.infrastructure.persistence.manner.entity.MannerItemEntity
import com.kobeinyourpocket.backend.infrastructure.persistence.manner.entity.MannerItemLocalizationEntity
import com.kobeinyourpocket.backend.infrastructure.persistence.manner.entity.MannerItemLocalizationId
import com.kobeinyourpocket.backend.infrastructure.persistence.manner.entity.MannerItemSpotEntity
import com.kobeinyourpocket.backend.infrastructure.persistence.manner.entity.MannerItemSpotId
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional

interface MannerItemJpaRepository : JpaRepository<MannerItemEntity, String> {
    /**
     * 更新用に行ロックを取って取得する（スポットの `findByIdForUpdate` と同じ）。
     *
     * 同じ項目への同時更新を直列化する。ロックが無いと後勝ちで先行変更が消えるうえ、
     * 古い読み取りを基準に「差し替えられた」と誤判定して**まだ参照されている画像を
     * 清理対象へ戻す**恐れがある。呼び出しトランザクションが終わるまでロックを保持する。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from MannerItemEntity m where m.id = :id")
    fun findByIdForUpdate(
        @Param("id") id: String,
    ): Optional<MannerItemEntity>

    /**
     * 削除して**消した件数を返す**。存在しなければ 0。
     *
     * 継承した `deleteById` は対象が無くても黙って何もしないため、事前に存在確認すると
     * 確認と削除の間に別リクエストが消した場合を取りこぼす（両方が 204 を返す）。
     * 1 文で削除し、その結果で存在を判定する。
     *
     * 子行（localization / spot）は V5 の ON DELETE CASCADE で DB 側が連動削除する。
     */
    @Modifying
    @Query("delete from MannerItemEntity m where m.id = :id")
    fun deleteByIdReturningCount(
        @Param("id") id: String,
    ): Int
}

interface MannerItemLocalizationJpaRepository : JpaRepository<MannerItemLocalizationEntity, MannerItemLocalizationId> {
    fun findByIdMannerItemId(mannerItemId: String): List<MannerItemLocalizationEntity>

    fun deleteByIdMannerItemId(mannerItemId: String)
}

interface MannerItemSpotJpaRepository : JpaRepository<MannerItemSpotEntity, MannerItemSpotId> {
    fun findByIdMannerItemId(mannerItemId: String): List<MannerItemSpotEntity>

    fun deleteByIdMannerItemId(mannerItemId: String)
}

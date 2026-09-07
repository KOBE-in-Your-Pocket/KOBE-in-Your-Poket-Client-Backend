package com.kobeinyourpocket.backend.infrastructure.persistence.manner.repository

import com.kobeinyourpocket.backend.infrastructure.persistence.manner.entity.MannerItemEntity
import com.kobeinyourpocket.backend.infrastructure.persistence.manner.entity.MannerItemLocalizationEntity
import com.kobeinyourpocket.backend.infrastructure.persistence.manner.entity.MannerItemLocalizationId
import com.kobeinyourpocket.backend.infrastructure.persistence.manner.entity.MannerItemSpotEntity
import com.kobeinyourpocket.backend.infrastructure.persistence.manner.entity.MannerItemSpotId
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
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
}

interface MannerItemLocalizationJpaRepository : JpaRepository<MannerItemLocalizationEntity, MannerItemLocalizationId> {
    fun findByIdMannerItemId(mannerItemId: String): List<MannerItemLocalizationEntity>

    fun deleteByIdMannerItemId(mannerItemId: String)
}

interface MannerItemSpotJpaRepository : JpaRepository<MannerItemSpotEntity, MannerItemSpotId> {
    fun findByIdMannerItemId(mannerItemId: String): List<MannerItemSpotEntity>

    fun deleteByIdMannerItemId(mannerItemId: String)
}

package com.kobeinyourpocket.backend.infrastructure.persistence.tourism

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional

interface SpotJpaRepository : JpaRepository<SpotEntity, String> {
    /**
     * 行ロック（SELECT ... FOR UPDATE）付きで 1 件取得する。
     *
     * 同じスポットへの同時更新を直列化するためのもの。呼び出し側のトランザクションが
     * 終わるまでロックを保持する。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SpotEntity s where s.id = :id")
    fun findByIdForUpdate(
        @Param("id") id: String,
    ): Optional<SpotEntity>
}

interface SpotLocalizationJpaRepository : JpaRepository<SpotLocalizationEntity, SpotLocalizationId>

package com.kobeinyourpocket.backend.infrastructure.persistence.tourism

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface FavoriteJpaRepository : JpaRepository<FavoriteEntity, FavoriteKey> {
    /**
     * 指定した利用者のお気に入りを一括削除する（退会処理 / #238）。
     *
     * derived query（`deleteBy...`）は 1 件ずつ読み込んでから消すため、JPQL の一括削除にする。
     *
     * @return 削除した件数
     */
    @Modifying
    @Query("DELETE FROM FavoriteEntity f WHERE f.id.userId = :userId")
    fun deleteByUserId(
        @Param("userId") userId: UUID,
    ): Int
}

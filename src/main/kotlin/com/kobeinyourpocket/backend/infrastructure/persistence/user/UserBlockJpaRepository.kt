package com.kobeinyourpocket.backend.infrastructure.persistence.user

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface UserBlockJpaRepository : JpaRepository<UserBlockEntity, UserBlockId> {
    /**
     * 指定したユーザーがした・されたブロックを一括削除する（退会処理）。
     *
     * 両方向をまとめて消すため derived query ではなく JPQL の一括削除にする。
     *
     * @return 削除した件数
     */
    @Modifying
    @Query("DELETE FROM UserBlockEntity b WHERE b.id.blockerUserId = :userId OR b.id.blockedUserId = :userId")
    fun deleteByUserId(
        @Param("userId") userId: UUID,
    ): Int
}

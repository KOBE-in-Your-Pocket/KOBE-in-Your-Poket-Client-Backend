package com.kobeinyourpocket.backend.infrastructure.persistence.tourism

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ReviewJpaRepository : JpaRepository<ReviewEntity, UUID> {
    /**
     * 投稿者の user id でレビューを一括削除する（退会処理 / #528）。
     *
     * 1 ユーザー分の投稿数は少ないため derived delete query で足りる。
     *
     * @return 削除した件数
     */
    fun deleteByAuthorUserId(authorUserId: UUID): Int
}

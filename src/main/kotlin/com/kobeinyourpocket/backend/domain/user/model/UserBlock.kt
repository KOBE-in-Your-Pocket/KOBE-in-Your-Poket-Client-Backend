package com.kobeinyourpocket.backend.domain.user.model

import java.time.Instant

/**
 * [エンティティ] 利用者によるブロック（#215）。
 *
 * App Store Guideline 1.2 の「悪質なユーザーを利用者がブロックできる仕組み」。
 * ブロックした側（[blockerId]）には、ブロックされた側（[blockedId]）のレビューを表示しない。
 *
 * 不変条件:
 * - 自分自身はブロックできない
 *
 * 同一性は [blockerId] と [blockedId] の組で決まる。同じ相手を 2 回ブロックしても 1 件として扱う。
 */
data class UserBlock(
    val blockerId: User.Id,
    val blockedId: User.Id,
    val createdAt: Instant,
) {
    init {
        require(blockerId != blockedId) { "user cannot block themselves: $blockerId" }
    }

    companion object {
        fun create(
            blockerId: User.Id,
            blockedId: User.Id,
            createdAt: Instant = Instant.now(),
        ): UserBlock = UserBlock(blockerId = blockerId, blockedId = blockedId, createdAt = createdAt)
    }
}

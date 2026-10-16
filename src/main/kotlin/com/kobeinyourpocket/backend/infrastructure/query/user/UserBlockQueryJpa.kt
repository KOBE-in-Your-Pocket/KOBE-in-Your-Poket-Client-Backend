package com.kobeinyourpocket.backend.infrastructure.query.user

import com.kobeinyourpocket.backend.application.user.query.BlockedUserView
import com.kobeinyourpocket.backend.application.user.query.UserBlockQuery
import com.kobeinyourpocket.backend.domain.user.model.User
import com.kobeinyourpocket.backend.infrastructure.query.common.JdbcTimestamps
import com.kobeinyourpocket.backend.infrastructure.query.common.JdbcUuids
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository

/** [UserBlockQuery] の JPA 実装。ブロック中のユーザーをプロフィールと結合して返す（#215）。 */
@Repository
class UserBlockQueryJpa(
    private val entityManager: EntityManager,
) : UserBlockQuery {
    override fun findBlockedUsers(blockerId: User.Id): List<BlockedUserView> {
        @Suppress("UNCHECKED_CAST")
        val rows =
            entityManager
                .createNativeQuery(SELECT_BLOCKED_USERS)
                .setParameter("blockerId", blockerId.value)
                .resultList as List<Array<Any?>>

        return rows.map(::toView)
    }

    /** id は uuid 列。ドライバごとに UUID / byte[] と型が割れるため [JdbcUuids] で吸収する。 */
    private fun toView(row: Array<Any?>): BlockedUserView =
        BlockedUserView(
            id = JdbcUuids.toUuidString(row[Column.ID]),
            name = row[Column.NAME] as String,
            iconUrl = (row[Column.ICON_URL] as String).ifEmpty { null },
            blockedAt = JdbcTimestamps.toInstant(row[Column.BLOCKED_AT]),
        )

    /** [SELECT_BLOCKED_USERS] の列順と対応する index。列の並び替え時は両方を合わせて更新すること。 */
    private object Column {
        const val ID = 0
        const val NAME = 1
        const val ICON_URL = 2
        const val BLOCKED_AT = 3
    }

    private companion object {
        /**
         * `INNER JOIN` にしているのは、退会済みのユーザーを一覧に出さないため。
         * 退会時にブロックの行も消すため通常は残らないが、表示名が出せない行は解除 UI でも扱えない。
         *
         * 同時刻の行の順序を固定するため、一意な id を第 2 キーに置く。
         */
        val SELECT_BLOCKED_USERS =
            """
            SELECT u.id, u.name, u.icon_url, b.created_at
            FROM user_blocks b
            JOIN users u ON u.id = b.blocked_user_id
            WHERE b.blocker_user_id = :blockerId
            ORDER BY b.created_at DESC, u.id
            """.trimIndent()
    }
}

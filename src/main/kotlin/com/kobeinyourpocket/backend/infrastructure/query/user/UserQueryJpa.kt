package com.kobeinyourpocket.backend.infrastructure.query.user

import com.kobeinyourpocket.backend.application.user.query.UserListItemView
import com.kobeinyourpocket.backend.application.user.query.UserPageView
import com.kobeinyourpocket.backend.application.user.query.UserQuery
import com.kobeinyourpocket.backend.infrastructure.query.common.JdbcTimestamps
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository

/** [UserQuery] の JPA 実装。users プロフィールテーブルを新しい順にページングして返す（#151）。 */
@Repository
class UserQueryJpa(
    private val entityManager: EntityManager,
) : UserQuery {
    override fun findPage(
        page: Int,
        size: Int,
    ): UserPageView {
        @Suppress("UNCHECKED_CAST")
        val rows =
            entityManager
                .createNativeQuery(SELECT_USERS)
                .setParameter("limit", size)
                .setParameter("offset", page.toLong() * size)
                .resultList as List<Array<Any?>>

        return UserPageView(
            users = rows.map(::toView),
            page = page,
            size = size,
            totalElements = (entityManager.createNativeQuery(COUNT_USERS).singleResult as Number).toLong(),
        )
    }

    /** id は uuid 列。ドライバごとに UUID / String と型が割れるため [toString] で吸収する。 */
    private fun toView(row: Array<Any?>): UserListItemView =
        UserListItemView(
            id = row[Column.ID].toString(),
            name = row[Column.NAME] as String,
            iconUrl = (row[Column.ICON_URL] as String).ifEmpty { null },
            createdAt = JdbcTimestamps.toInstant(row[Column.CREATED_AT]),
        )

    /** [SELECT_USERS] の列順と対応する index。列の並び替え時は両方を合わせて更新すること。 */
    private object Column {
        const val ID = 0
        const val NAME = 1
        const val ICON_URL = 2
        const val CREATED_AT = 3
    }

    private companion object {
        /**
         * created_at だけでは同時刻の行の順序が不定になり、ページ間で行が重複・欠落する。
         * 一意な id を第2キーに置いて全順序にする。
         */
        val SELECT_USERS =
            """
            SELECT id, name, icon_url, created_at
            FROM users
            ORDER BY created_at DESC, id
            LIMIT :limit OFFSET :offset
            """.trimIndent()

        val COUNT_USERS =
            """
            SELECT count(*)
            FROM users
            """.trimIndent()
    }
}

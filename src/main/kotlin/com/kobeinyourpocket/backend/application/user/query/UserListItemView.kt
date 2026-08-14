package com.kobeinyourpocket.backend.application.user.query

import java.time.Instant

/**
 * 運営向けユーザー一覧の 1 件分の読みモデル（#151）。
 *
 * CQRS read 側専用。command 側の集約 [com.kobeinyourpocket.backend.domain.user.model.User] とは別経路。
 *
 * ロールは含めない。ロールの正は Supabase Auth の `app_metadata.role` であり
 * users テーブルには無いため、一覧に載せると 1 件ごとに Admin API 呼び出しが必要になる
 * （[com.kobeinyourpocket.backend.domain.user.vo.Role] のコメント参照）。
 */
data class UserListItemView(
    val id: String,
    val name: String,
    val iconUrl: String?,
    val createdAt: Instant,
)

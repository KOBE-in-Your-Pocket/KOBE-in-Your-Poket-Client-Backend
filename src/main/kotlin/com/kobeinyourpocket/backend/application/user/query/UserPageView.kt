package com.kobeinyourpocket.backend.application.user.query

/**
 * ユーザー一覧の 1 ページ分の読みモデル（#151）。
 *
 * 総件数を持つのは、管理画面が「全 N 件中 M 件目」を出すため。
 * 一覧そのものは [users] だけで足りるが、ページャは総数が無いと描けない。
 */
data class UserPageView(
    val users: List<UserListItemView>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
) {
    /** 総ページ数。[size] が 0 以下になる経路は [ListUsersService] が塞ぐ。 */
    val totalPages: Int
        get() = if (size <= 0) 0 else ((totalElements + size - 1) / size).toInt()
}

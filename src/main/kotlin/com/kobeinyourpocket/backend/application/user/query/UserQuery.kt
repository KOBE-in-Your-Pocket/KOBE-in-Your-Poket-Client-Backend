package com.kobeinyourpocket.backend.application.user.query

/** read 専用 port。application が定義し infrastructure.query が実装する。 */
interface UserQuery {
    /**
     * ユーザーを新しい順に 1 ページ分取得する。
     *
     * [page] は 0 始まり。境界の正規化（既定値・上限）は [ListUsersService] の責務で、
     * ここへは正規化済みの値だけが渡る。
     */
    fun findPage(
        page: Int,
        size: Int,
    ): UserPageView
}

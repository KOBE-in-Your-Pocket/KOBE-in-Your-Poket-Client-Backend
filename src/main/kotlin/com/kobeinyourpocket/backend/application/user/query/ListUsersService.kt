package com.kobeinyourpocket.backend.application.user.query

import org.springframework.stereotype.Service

/**
 * 運営向けユーザー一覧取得ユースケース（read / #151）。domain 集約を経由せず [UserQuery] port へ委譲する。
 *
 * ページ境界の正規化をここに置く。REST 層の既定値と application の上限が二重管理になると、
 * 別の入口（将来の内部呼び出し等）から上限を超えた size が素通りするため。
 */
@Service
class ListUsersService(
    private val userQuery: UserQuery,
) {
    /**
     * [page]（0 始まり）/ [size] は未指定可。負値は下限へ、上限超過は [MAX_SIZE] へ丸める。
     *
     * 不正値を 400 で弾かず丸めているのは、一覧取得が破壊的でなく、
     * 管理画面のページャ操作を落とすより安全側の結果を返す方が実用的なため。
     */
    fun listUsers(
        page: Int? = null,
        size: Int? = null,
    ): UserPageView =
        userQuery.findPage(
            page = (page ?: 0).coerceAtLeast(0),
            size = (size ?: DEFAULT_SIZE).coerceIn(1, MAX_SIZE),
        )

    companion object {
        /** 管理画面の 1 画面分として十分な件数。 */
        const val DEFAULT_SIZE = 50

        /** 1 リクエストで返す上限。全件取得による重いレスポンスを防ぐ。 */
        const val MAX_SIZE = 200
    }
}

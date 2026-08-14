package com.kobeinyourpocket.backend.application.user.query

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * ページ境界の正規化（#151）。
 *
 * 管理画面のページャが不正値を送っても一覧が落ちないこと、
 * および全件取得で重いレスポンスにならないことを押さえる。
 */
class ListUsersServiceTest {
    /** 正規化後の引数を記録するだけの [UserQuery]。 */
    private class RecordingUserQuery : UserQuery {
        var requestedPage: Int? = null
        var requestedSize: Int? = null

        override fun findPage(
            page: Int,
            size: Int,
        ): UserPageView {
            requestedPage = page
            requestedSize = size
            return UserPageView(users = emptyList(), page = page, size = size, totalElements = 0)
        }
    }

    private val userQuery = RecordingUserQuery()
    private val service = ListUsersService(userQuery)

    @Test
    fun `未指定なら先頭ページと既定件数を使う`() {
        service.listUsers()

        assertEquals(0, userQuery.requestedPage)
        assertEquals(ListUsersService.DEFAULT_SIZE, userQuery.requestedSize)
    }

    @Test
    fun `負の page は先頭ページに丸める`() {
        service.listUsers(page = -1)

        assertEquals(0, userQuery.requestedPage)
    }

    @Test
    fun `0 以下の size は 1 件に丸める`() {
        service.listUsers(size = 0)

        assertEquals(1, userQuery.requestedSize)
    }

    @Test
    fun `上限を超える size は MAX_SIZE に丸める`() {
        service.listUsers(size = ListUsersService.MAX_SIZE + 1)

        assertEquals(ListUsersService.MAX_SIZE, userQuery.requestedSize)
    }

    @Test
    fun `範囲内の指定はそのまま渡す`() {
        service.listUsers(page = 2, size = 10)

        assertEquals(2, userQuery.requestedPage)
        assertEquals(10, userQuery.requestedSize)
    }

    @Test
    fun `totalPages は端数を切り上げる`() {
        assertEquals(3, UserPageView(users = emptyList(), page = 0, size = 10, totalElements = 21).totalPages)
    }

    @Test
    fun `0 件なら totalPages は 0`() {
        assertEquals(0, UserPageView(users = emptyList(), page = 0, size = 10, totalElements = 0).totalPages)
    }
}

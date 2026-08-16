package com.kobeinyourpocket.backend.infrastructure.query.user

import com.kobeinyourpocket.backend.domain.user.model.User
import com.kobeinyourpocket.backend.domain.user.repository.UserRepository
import com.kobeinyourpocket.backend.domain.user.vo.UserIcon
import com.kobeinyourpocket.backend.infrastructure.persistence.user.UserRepositoryImpl
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** ユーザー一覧の read クエリ（#151）。並び順・ページング・総件数を押さえる。 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(UserRepositoryImpl::class, UserQueryJpa::class)
class UserQueryJpaTest {
    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var userQuery: UserQueryJpa

    private val base = Instant.parse("2026-08-01T00:00:00Z")

    private fun save(
        name: String,
        createdAt: Instant,
        icon: UserIcon? = null,
    ) {
        userRepository.save(
            User.create(id = User.Id.of(UUID.randomUUID()), name = name, icon = icon, createdAt = createdAt),
        )
    }

    @Test
    fun `登録が新しい順に返る`() {
        save("古い", base)
        save("新しい", base.plusSeconds(60))

        val result = userQuery.findPage(page = 0, size = 10)

        assertEquals(listOf("新しい", "古い"), result.users.map { it.name })
    }

    @Test
    fun `size で絞っても totalElements は全件を返す`() {
        save("a", base)
        save("b", base.plusSeconds(1))
        save("c", base.plusSeconds(2))

        val result = userQuery.findPage(page = 0, size = 2)

        assertEquals(listOf("c", "b"), result.users.map { it.name })
        assertEquals(3L, result.totalElements)
        assertEquals(2, result.totalPages)
    }

    @Test
    fun `page で次のページを取得できる`() {
        save("a", base)
        save("b", base.plusSeconds(1))
        save("c", base.plusSeconds(2))

        val result = userQuery.findPage(page = 1, size = 2)

        assertEquals(listOf("a"), result.users.map { it.name })
        assertEquals(1, result.page)
    }

    @Test
    fun `範囲外の page は空になるが総件数は返る`() {
        save("a", base)

        val result = userQuery.findPage(page = 5, size = 10)

        assertTrue(result.users.isEmpty())
        assertEquals(1L, result.totalElements)
    }

    @Test
    fun `アイコン未設定は iconUrl を null にする`() {
        save("アイコンなし", base)

        val result = userQuery.findPage(page = 0, size = 10)

        assertNull(result.users.single().iconUrl)
    }

    @Test
    fun `アイコン設定済みは URL をそのまま返す`() {
        save("アイコンあり", base, UserIcon.of("https://example.com/icon.png"))

        val result = userQuery.findPage(page = 0, size = 10)

        assertEquals("https://example.com/icon.png", result.users.single().iconUrl)
    }

    @Test
    fun `createdAt を Instant として返す`() {
        save("時刻確認", base)

        val result = userQuery.findPage(page = 0, size = 10)

        assertEquals(base, result.users.single().createdAt)
    }

    @Test
    fun `1 件も無ければ空ページを返す`() {
        val result = userQuery.findPage(page = 0, size = 10)

        assertTrue(result.users.isEmpty())
        assertEquals(0L, result.totalElements)
        assertEquals(0, result.totalPages)
    }
}

package com.kobeinyourpocket.backend.domain.user

import com.kobeinyourpocket.backend.domain.user.model.User
import com.kobeinyourpocket.backend.domain.user.vo.PublicUser
import com.kobeinyourpocket.backend.domain.user.vo.Role
import com.kobeinyourpocket.backend.domain.user.vo.UserIcon
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class UserIdTest {
    @Test
    fun `UUID 文字列から復元できる`() {
        val uuid = UUID.randomUUID()
        val id = User.Id.of(uuid.toString())

        assertEquals(uuid, id.value)
    }

    @Test
    fun `UUID インスタンスから生成できる`() {
        val uuid = UUID.randomUUID()
        val id = User.Id.of(uuid)

        assertEquals(uuid, id.value)
    }

    @Test
    fun `不正な UUID 文字列は拒否する`() {
        assertFailsWith<IllegalArgumentException> {
            User.Id.of("not-a-uuid")
        }
    }
}

class RoleTest {
    @Test
    fun `claim 文字列から一般・運営を解決できる`() {
        assertEquals(Role.GENERAL, Role.of("general"))
        assertEquals(Role.OPERATOR, Role.of("operator"))
        assertEquals(Role.GENERAL, Role.of(" GENERAL "))
    }

    @Test
    fun `未対応・空の claim は null`() {
        assertNull(Role.of(""))
        assertNull(Role.of("   "))
        assertNull(Role.of("admin"))
    }
}

class UserIconTest {
    @Test
    fun `URL から生成できる`() {
        val icon = UserIcon.of("https://example.com/alice.png")

        assertEquals("https://example.com/alice.png", icon.url)
    }

    @Test
    fun `前後空白は trim される`() {
        assertEquals("https://example.com/a.png", UserIcon.of("  https://example.com/a.png  ").url)
    }

    @Test
    fun `空白のみなら拒否する`() {
        assertFailsWith<IllegalArgumentException> {
            UserIcon.of("   ")
        }
    }
}

class PublicUserTest {
    @Test
    fun `name と iconUrl を指定して生成できる`() {
        val user = PublicUser(name = "Alice", iconUrl = "https://example.com/alice.png")

        assertEquals("Alice", user.name)
        assertEquals("https://example.com/alice.png", user.iconUrl)
    }

    @Test
    fun `iconUrl は省略できる`() {
        assertNull(PublicUser(name = "Alice").iconUrl)
    }

    @Test
    fun `name が空白のみなら拒否する`() {
        assertFailsWith<IllegalArgumentException> {
            PublicUser(name = "   ")
        }
    }

    @Test
    fun `name が上限を超えたら拒否する`() {
        assertFailsWith<IllegalArgumentException> {
            PublicUser(name = "a".repeat(PublicUser.MAX_NAME_LENGTH + 1))
        }
    }
}

class UserDomainTest {
    private val userId = User.Id.of(UUID.fromString("11111111-1111-1111-1111-111111111111"))
    private val now = Instant.parse("2026-07-13T02:00:00Z")
    private val aliceIcon = UserIcon.of("https://example.com/alice.png")

    @Test
    fun `create でプロフィールを生成できる`() {
        val user =
            User.create(
                id = userId,
                name = "Alice",
                icon = aliceIcon,
                createdAt = now,
            )

        assertEquals(userId, user.id)
        assertEquals("Alice", user.name)
        assertEquals(aliceIcon, user.icon)
        assertEquals(now, user.createdAt)
        assertEquals(now, user.updatedAt)
    }

    @Test
    fun `toPublicUser は name と iconUrl のみを射影する`() {
        val user =
            User.create(
                id = userId,
                name = "Alice",
                icon = aliceIcon,
                createdAt = now,
            )

        assertEquals(PublicUser(name = "Alice", iconUrl = "https://example.com/alice.png"), user.toPublicUser())
    }

    @Test
    fun `updateProfile は表示名を更新し id と createdAt を維持する`() {
        val user =
            User.create(
                id = userId,
                name = "Alice",
                createdAt = now,
            )
        val updatedAt = Instant.parse("2026-07-13T03:00:00Z")
        val newIcon = UserIcon.of("https://example.com/new.png")

        val updated = user.updateProfile(name = "Alice Updated", icon = newIcon, updatedAt = updatedAt)

        assertEquals(userId, updated.id)
        assertEquals("Alice Updated", updated.name)
        assertEquals(newIcon, updated.icon)
        assertEquals(now, updated.createdAt)
        assertEquals(updatedAt, updated.updatedAt)
    }

    @Test
    fun `name が空白のみなら拒否する`() {
        assertFailsWith<IllegalArgumentException> {
            User.create(id = userId, name = "   ", createdAt = now)
        }
    }

    @Test
    fun `name が上限を超えたら拒否する`() {
        assertFailsWith<IllegalArgumentException> {
            User.create(id = userId, name = "a".repeat(User.MAX_NAME_LENGTH + 1), createdAt = now)
        }
    }
}

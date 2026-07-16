package com.kobeinyourpocket.backend.domain.user

import com.kobeinyourpocket.backend.domain.user.model.User
import com.kobeinyourpocket.backend.domain.user.repository.UserRepository
import com.kobeinyourpocket.backend.domain.user.vo.UserIcon
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** UserRepository write port の契約を Fake で検証する。 */
class UserRepositoryPortTest {
    private class FakeUserRepository : UserRepository {
        private val store = linkedMapOf<User.Id, User>()

        override fun save(user: User): User {
            store[user.id] = user
            return user
        }

        override fun findById(id: User.Id): User? = store[id]

        override fun deleteById(id: User.Id) {
            store.remove(id)
        }
    }

    private val now = Instant.parse("2026-07-13T02:00:00Z")

    @Test
    fun `save した User を取得できる`() {
        val repository = FakeUserRepository()
        val user =
            User.create(
                id = User.Id.of(UUID.fromString("11111111-1111-1111-1111-111111111111")),
                name = "Alice",
                icon = UserIcon.of("https://example.com/alice.png"),
                createdAt = now,
            )

        repository.save(user)

        val found = repository.findById(user.id)!!
        assertEquals(user.id, found.id)
        assertEquals("Alice", found.name)
        assertEquals(UserIcon.of("https://example.com/alice.png"), found.icon)
        assertEquals(now, found.createdAt)
        assertEquals(now, found.updatedAt)
    }

    @Test
    fun `未保存の id は null`() {
        val repository = FakeUserRepository()

        assertNull(repository.findById(User.Id.of(UUID.randomUUID())))
    }

    @Test
    fun `deleteById で保存済みの User を削除できる`() {
        val repository = FakeUserRepository()
        val user =
            User.create(
                id = User.Id.of(UUID.fromString("11111111-1111-1111-1111-111111111111")),
                name = "Alice",
                createdAt = now,
            )
        repository.save(user)
        assertNotNull(repository.findById(user.id))

        repository.deleteById(user.id)

        assertNull(repository.findById(user.id))
    }

    @Test
    fun `異なる User をそれぞれ保存できる`() {
        val repository = FakeUserRepository()
        val alice =
            User.create(
                id = User.Id.of(UUID.fromString("11111111-1111-1111-1111-111111111111")),
                name = "Alice",
                createdAt = now,
            )
        val bob =
            User.create(
                id = User.Id.of(UUID.fromString("22222222-2222-2222-2222-222222222222")),
                name = "Bob",
                createdAt = now,
            )

        repository.save(alice)
        repository.save(bob)

        assertEquals("Alice", repository.findById(alice.id)!!.name)
        assertEquals("Bob", repository.findById(bob.id)!!.name)
    }
}

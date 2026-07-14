package com.kobeinyourpocket.backend.infrastructure.persistence.user

import com.kobeinyourpocket.backend.domain.user.model.User
import com.kobeinyourpocket.backend.domain.user.repository.UserRepository
import com.kobeinyourpocket.backend.domain.user.vo.UserIcon
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

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(UserRepositoryImpl::class)
class UserRepositoryImplTest {
    @Autowired
    private lateinit var repository: UserRepository

    @Autowired
    private lateinit var userJpa: UserJpaRepository

    private val userId = User.Id.of(UUID.fromString("11111111-1111-1111-1111-111111111111"))
    private val now = Instant.parse("2026-07-13T02:00:00Z")

    @Test
    fun `save で users が永続化される`() {
        val user =
            User.create(
                id = userId,
                name = "Alice",
                icon = UserIcon.of("https://example.com/alice.png"),
                createdAt = now,
            )

        repository.save(user)

        val entity = userJpa.findById(user.id.value).orElseThrow()
        assertEquals("Alice", entity.name)
        assertEquals("https://example.com/alice.png", entity.iconUrl)
        assertEquals(now, entity.createdAt)
        assertEquals(now, entity.updatedAt)
    }

    @Test
    fun `icon なしの User も永続化できる`() {
        val user =
            User.create(
                id = userId,
                name = "Bob",
                createdAt = now,
            )

        repository.save(user)

        val entity = userJpa.findById(user.id.value).orElseThrow()
        assertEquals("", entity.iconUrl)
        assertNull(repository.findById(userId)!!.icon)
    }

    @Test
    fun `findById で domain に復元できる`() {
        val user =
            User.create(
                id = userId,
                name = "Carol",
                icon = UserIcon.of("https://example.com/carol.png"),
                createdAt = now,
                updatedAt = now,
            )
        repository.save(user)

        assertEquals(user, repository.findById(userId))
    }

    @Test
    fun `未保存の id は null`() {
        assertNull(repository.findById(User.Id.of(UUID.randomUUID())))
    }
}

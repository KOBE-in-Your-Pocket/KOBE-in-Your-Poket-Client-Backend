package com.kobeinyourpocket.backend.infrastructure.persistence.user

import com.kobeinyourpocket.backend.domain.user.model.User
import com.kobeinyourpocket.backend.domain.user.repository.AppleRefreshTokenRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(AppleRefreshTokenRepositoryImpl::class)
class AppleRefreshTokenRepositoryImplTest {
    @Autowired
    private lateinit var repository: AppleRefreshTokenRepository

    private val userId = User.Id.of(UUID.fromString("11111111-1111-1111-1111-111111111111"))

    @Test
    fun `保存した refresh token を読み出せる`() {
        repository.save(userId, "apple-refresh")

        assertEquals("apple-refresh", repository.findByUserId(userId))
    }

    @Test
    fun `同じユーザーで保存し直すと最新の値に置き換わる`() {
        repository.save(userId, "old-refresh")
        repository.save(userId, "new-refresh")

        assertEquals("new-refresh", repository.findByUserId(userId))
    }

    @Test
    fun `削除すると読み出せなくなる`() {
        repository.save(userId, "apple-refresh")

        repository.deleteByUserId(userId)

        assertNull(repository.findByUserId(userId))
    }

    @Test
    fun `未保存なら null を返し、削除しても失敗しない`() {
        assertNull(repository.findByUserId(userId))

        repository.deleteByUserId(userId)
    }
}

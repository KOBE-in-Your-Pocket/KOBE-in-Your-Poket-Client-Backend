package com.kobeinyourpocket.backend.application.user.query

import com.kobeinyourpocket.backend.application.user.command.UserNotFoundException
import com.kobeinyourpocket.backend.domain.user.model.User
import com.kobeinyourpocket.backend.domain.user.repository.UserRepository
import com.kobeinyourpocket.backend.domain.user.vo.UserIcon
import io.mockk.every
import io.mockk.mockk
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class GetMeServiceTest {
    private val userRepository = mockk<UserRepository>()
    private val service = GetMeService(userRepository)

    private val userId = User.Id.of(UUID.fromString("11111111-1111-1111-1111-111111111111"))

    @Test
    fun `プロフィールがあれば PublicUser 射影を返す`() {
        val user = User.create(id = userId, name = "Alice", icon = UserIcon.of("https://example.com/icon.png"))
        every { userRepository.findById(userId) } returns user

        val result = service.execute(userId)

        assertEquals(userId, result.id)
        assertEquals("Alice", result.name)
        assertEquals("https://example.com/icon.png", result.iconUrl)
    }

    @Test
    fun `アイコン未設定なら iconUrl は null`() {
        every { userRepository.findById(userId) } returns User.create(id = userId, name = "Alice")

        assertNull(service.execute(userId).iconUrl)
    }

    @Test
    fun `プロフィールが無ければ UserNotFoundException`() {
        every { userRepository.findById(userId) } returns null

        assertFailsWith<UserNotFoundException> { service.execute(userId) }
    }
}

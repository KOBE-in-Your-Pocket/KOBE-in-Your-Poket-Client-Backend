package com.kobeinyourpocket.backend.application.user.command

import com.kobeinyourpocket.backend.application.user.auth.AuthGateway
import com.kobeinyourpocket.backend.application.user.auth.AuthGatewayException
import com.kobeinyourpocket.backend.domain.user.model.User
import com.kobeinyourpocket.backend.domain.user.repository.UserRepository
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertFailsWith

class DeleteUserServiceTest {
    private val authGateway = mockk<AuthGateway>()
    private val userRepository = mockk<UserRepository>()
    private val service = DeleteUserService(authGateway, userRepository)

    private val userId = User.Id.of(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"))
    private val existingUser = User.create(id = userId, name = "Alice")

    @Test
    fun `存在するユーザーを削除すると Auth と DB の両方が削除される`() {
        every { userRepository.findById(userId) } returns existingUser
        justRun { authGateway.deleteUser(userId) }
        justRun { userRepository.deleteById(userId) }

        service.execute(userId)

        verify(exactly = 1) { authGateway.deleteUser(userId) }
        verify(exactly = 1) { userRepository.deleteById(userId) }
    }

    @Test
    fun `存在しないユーザー ID を指定すると UserNotFoundException`() {
        every { userRepository.findById(userId) } returns null

        assertFailsWith<UserNotFoundException> {
            service.execute(userId)
        }

        verify(exactly = 0) { authGateway.deleteUser(any()) }
        verify(exactly = 0) { userRepository.deleteById(any()) }
    }

    @Test
    fun `Supabase Admin API が失敗したら AuthGatewayException が伝播し DB 削除は実行されない`() {
        every { userRepository.findById(userId) } returns existingUser
        every { authGateway.deleteUser(userId) } throws
            AuthGatewayException(status = 500, message = "Supabase internal error")

        assertFailsWith<AuthGatewayException> {
            service.execute(userId)
        }

        verify(exactly = 0) { userRepository.deleteById(any()) }
    }
}

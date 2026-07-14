package com.kobeinyourpocket.backend.application.user.command

import com.kobeinyourpocket.backend.application.user.auth.AuthGateway
import com.kobeinyourpocket.backend.application.user.auth.AuthSession
import com.kobeinyourpocket.backend.domain.user.model.User
import com.kobeinyourpocket.backend.domain.user.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SignUpServiceTest {
    private val authGateway = mockk<AuthGateway>()
    private val userRepository = mockk<UserRepository>(relaxed = true)
    private val service = SignUpService(authGateway, userRepository)

    private val userId = User.Id.of(UUID.fromString("11111111-1111-1111-1111-111111111111"))

    @Test
    fun `signup 成功後にプロフィールを保存する`() {
        every { authGateway.signUp("a@example.com", "password1") } returns
            AuthSession(
                userId = userId,
                accessToken = "access",
                refreshToken = "refresh",
                expiresIn = 3600,
                tokenType = "bearer",
            )
        every { userRepository.findById(userId) } returns null

        val result = service.execute(email = "a@example.com", password = "password1", name = "Alice")

        assertEquals(userId, result.user!!.id)
        assertEquals("Alice", result.user!!.name)
        assertEquals("access", result.session.accessToken)
        verify(exactly = 1) { userRepository.save(match { it.id == userId && it.name == "Alice" }) }
    }

    @Test
    fun `既にプロフィールがある場合は再作成しない`() {
        val existing = User.create(id = userId, name = "Existing")
        every { authGateway.signUp("a@example.com", "password1") } returns
            AuthSession(userId = userId, accessToken = "access", refreshToken = "r", expiresIn = 1, tokenType = "bearer")
        every { userRepository.findById(userId) } returns existing

        val result = service.execute(email = "a@example.com", password = "password1", name = "Alice")

        assertEquals("Existing", result.user!!.name)
        verify(exactly = 0) { userRepository.save(any()) }
    }
}

class SignInServiceTest {
    private val authGateway = mockk<AuthGateway>()
    private val userRepository = mockk<UserRepository>()
    private val service = SignInService(authGateway, userRepository)
    private val userId = User.Id.of(UUID.fromString("11111111-1111-1111-1111-111111111111"))

    @Test
    fun `プロフィールが無ければ user は null`() {
        every { authGateway.signInWithPassword("a@example.com", "password1") } returns
            AuthSession(userId = userId, accessToken = "a", refreshToken = "r", expiresIn = 1, tokenType = "bearer")
        every { userRepository.findById(userId) } returns null

        val result = service.execute("a@example.com", "password1")

        assertNull(result.user)
        assertEquals("a", result.session.accessToken)
    }
}

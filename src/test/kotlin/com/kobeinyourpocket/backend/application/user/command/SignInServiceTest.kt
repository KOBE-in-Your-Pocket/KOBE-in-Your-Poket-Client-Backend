package com.kobeinyourpocket.backend.application.user.command

import com.kobeinyourpocket.backend.application.user.auth.AuthGateway
import com.kobeinyourpocket.backend.application.user.auth.AuthGatewayException
import com.kobeinyourpocket.backend.application.user.auth.AuthSession
import com.kobeinyourpocket.backend.application.user.auth.LoginAttemptLimiter
import com.kobeinyourpocket.backend.application.user.auth.TooManyAuthAttemptsException
import com.kobeinyourpocket.backend.domain.user.model.User
import com.kobeinyourpocket.backend.domain.user.repository.UserRepository
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import java.time.Duration
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SignInServiceTest {
    private val authGateway = mockk<AuthGateway>()
    private val userRepository = mockk<UserRepository>(relaxed = true)
    private val ensureUserProfileService = EnsureUserProfileService(userRepository, UserProfileInserter(userRepository))
    private val loginAttemptLimiter = mockk<LoginAttemptLimiter>()
    private val service = SignInService(authGateway, ensureUserProfileService, loginAttemptLimiter)

    private val userId = User.Id.of(UUID.fromString("11111111-1111-1111-1111-111111111111"))
    private val ip = "203.0.113.1"

    // --- プロフィール補完（#89-b / #88） ---

    @Test
    fun `プロフィールが無ければ Auth id で冪等に補完する`() {
        justRun { loginAttemptLimiter.acquire(any(), any()) }
        justRun { loginAttemptLimiter.reset(any(), any()) }
        every { authGateway.signInWithPassword("a@example.com", "password1") } returns
            AuthSession(userId = userId, accessToken = "a", refreshToken = "r", expiresIn = 1, tokenType = "bearer")
        every { userRepository.findById(userId) } returns null

        val result = service.execute("a@example.com", "password1", ip)

        assertEquals(userId, result.user!!.id)
        assertEquals("a", result.user!!.name)
        assertEquals("a", result.session.accessToken)
        verify(exactly = 1) { userRepository.save(match { it.id == userId && it.name == "a" }) }
    }

    @Test
    fun `既存プロフィールがあれば再作成しない`() {
        justRun { loginAttemptLimiter.acquire(any(), any()) }
        justRun { loginAttemptLimiter.reset(any(), any()) }
        val existing = User.create(id = userId, name = "Alice")
        every { authGateway.signInWithPassword("a@example.com", "password1") } returns
            AuthSession(userId = userId, accessToken = "a", refreshToken = "r", expiresIn = 1, tokenType = "bearer")
        every { userRepository.findById(userId) } returns existing

        val result = service.execute("a@example.com", "password1", ip)

        assertEquals("Alice", result.user!!.name)
        verify(exactly = 0) { userRepository.save(any()) }
    }

    // --- 試行回数制限（#244） ---

    @Test
    fun `GoTrue を呼ぶ前に試行を数え、成功したら回数を消す`() {
        justRun { loginAttemptLimiter.acquire(ip, "a@example.com") }
        justRun { loginAttemptLimiter.reset(ip, "a@example.com") }
        every { authGateway.signInWithPassword("a@example.com", "password1") } returns
            AuthSession(userId = userId, accessToken = "access", refreshToken = "r", expiresIn = 1, tokenType = "bearer")
        every { userRepository.findById(userId) } returns User.create(id = userId, name = "Alice")

        val result = service.execute(email = " a@example.com ", password = "password1", clientIp = ip)

        assertEquals("access", result.session.accessToken)
        verifyOrder {
            loginAttemptLimiter.acquire(ip, "a@example.com")
            authGateway.signInWithPassword("a@example.com", "password1")
            loginAttemptLimiter.reset(ip, "a@example.com")
        }
    }

    @Test
    fun `ログインに失敗したら回数を消さない`() {
        justRun { loginAttemptLimiter.acquire(ip, "a@example.com") }
        every { authGateway.signInWithPassword("a@example.com", "bad") } throws
            AuthGatewayException(status = 400, message = "Invalid login credentials")

        assertFailsWith<AuthGatewayException> {
            service.execute(email = "a@example.com", password = "bad", clientIp = ip)
        }

        verify(exactly = 0) { loginAttemptLimiter.reset(any(), any()) }
    }

    @Test
    fun `上限に達していたら GoTrue を呼ばずに TooManyAuthAttemptsException`() {
        every { loginAttemptLimiter.acquire(ip, "a@example.com") } throws TooManyAuthAttemptsException(Duration.ofMinutes(15))

        assertFailsWith<TooManyAuthAttemptsException> {
            service.execute(email = "a@example.com", password = "password1", clientIp = ip)
        }

        verify(exactly = 0) { authGateway.signInWithPassword(any(), any()) }
    }
}

package com.kobeinyourpocket.backend.application.user.command

import com.kobeinyourpocket.backend.application.user.auth.AuthGateway
import com.kobeinyourpocket.backend.application.user.auth.AuthGatewayException
import com.kobeinyourpocket.backend.application.user.auth.AuthSession
import com.kobeinyourpocket.backend.domain.user.model.User
import com.kobeinyourpocket.backend.domain.user.repository.UserRepository
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SignUpServiceTest {
    private val authGateway = mockk<AuthGateway>()
    private val userRepository = mockk<UserRepository>(relaxed = true)
    private val ensureUserProfileService = EnsureUserProfileService(userRepository)
    private val service = SignUpService(authGateway, ensureUserProfileService)

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

    @Test
    fun `authGateway が AuthGatewayException を投げたらそのまま伝播する`() {
        every { authGateway.signUp("a@example.com", "password1") } throws
            AuthGatewayException(status = 422, message = "Signup failed")

        assertFailsWith<AuthGatewayException> {
            service.execute(email = "a@example.com", password = "password1", name = "Alice")
        }
    }

    @Test
    fun `プロフィール保存が一時失敗してもリトライして成功する`() {
        every { authGateway.signUp("a@example.com", "password1") } returns
            AuthSession(userId = userId, accessToken = "access", refreshToken = "r", expiresIn = 1, tokenType = "bearer")
        every { userRepository.findById(userId) } returns null andThen null
        every { userRepository.save(any()) } throws RuntimeException("transient") andThenAnswer { it.invocation.args[0] as User }

        val result = service.execute(email = "a@example.com", password = "password1", name = "Alice")

        assertEquals("Alice", result.user!!.name)
        verify(exactly = 2) { userRepository.save(any()) }
    }
}

class SignInServiceTest {
    private val authGateway = mockk<AuthGateway>()
    private val userRepository = mockk<UserRepository>(relaxed = true)
    private val ensureUserProfileService = EnsureUserProfileService(userRepository)
    private val service = SignInService(authGateway, ensureUserProfileService)
    private val userId = User.Id.of(UUID.fromString("11111111-1111-1111-1111-111111111111"))

    @Test
    fun `プロフィールが無ければ Auth id で冪等に補完する`() {
        every { authGateway.signInWithPassword("a@example.com", "password1") } returns
            AuthSession(userId = userId, accessToken = "a", refreshToken = "r", expiresIn = 1, tokenType = "bearer")
        every { userRepository.findById(userId) } returns null

        val result = service.execute("a@example.com", "password1")

        assertEquals(userId, result.user!!.id)
        assertEquals("a", result.user!!.name)
        assertEquals("a", result.session.accessToken)
        verify(exactly = 1) { userRepository.save(match { it.id == userId && it.name == "a" }) }
    }

    @Test
    fun `既存プロフィールがあれば再作成しない`() {
        val existing = User.create(id = userId, name = "Alice")
        every { authGateway.signInWithPassword("a@example.com", "password1") } returns
            AuthSession(userId = userId, accessToken = "a", refreshToken = "r", expiresIn = 1, tokenType = "bearer")
        every { userRepository.findById(userId) } returns existing

        val result = service.execute("a@example.com", "password1")

        assertEquals("Alice", result.user!!.name)
        verify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `authGateway が AuthGatewayException を投げたらそのまま伝播する`() {
        every { authGateway.signInWithPassword("a@example.com", "bad") } throws
            AuthGatewayException(status = 400, message = "Invalid login credentials")

        assertFailsWith<AuthGatewayException> {
            service.execute("a@example.com", "bad")
        }
    }
}

class RefreshSessionServiceTest {
    private val authGateway = mockk<AuthGateway>()
    private val userRepository = mockk<UserRepository>()
    private val service = RefreshSessionService(authGateway, userRepository)
    private val userId = User.Id.of(UUID.fromString("22222222-2222-2222-2222-222222222222"))

    @Test
    fun `リフレッシュ成功時にセッションと user を返す`() {
        val user = User.create(id = userId, name = "Bob")
        every { authGateway.refresh("rt") } returns
            AuthSession(userId = userId, accessToken = "new", refreshToken = "new-rt", expiresIn = 3600, tokenType = "bearer")
        every { userRepository.findById(userId) } returns user

        val result = service.execute("rt")

        assertEquals("new", result.session.accessToken)
        assertEquals("Bob", result.user!!.name)
    }

    @Test
    fun `authGateway が AuthGatewayException を投げたらそのまま伝播する`() {
        every { authGateway.refresh("expired") } throws
            AuthGatewayException(status = 401, message = "Invalid refresh token")

        assertFailsWith<AuthGatewayException> {
            service.execute("expired")
        }
    }
}

class SignOutServiceTest {
    private val authGateway = mockk<AuthGateway>()
    private val service = SignOutService(authGateway)

    @Test
    fun `signOut を authGateway へ委譲する`() {
        justRun { authGateway.signOut("token") }

        service.execute("token")

        verify(exactly = 1) { authGateway.signOut("token") }
    }

    @Test
    fun `authGateway が AuthGatewayException を投げたらそのまま伝播する`() {
        every { authGateway.signOut("bad") } throws
            AuthGatewayException(status = 401, message = "Invalid access token")

        assertFailsWith<AuthGatewayException> {
            service.execute("bad")
        }
    }
}

class EnsureUserProfileResilientTest {
    private val userRepository = mockk<UserRepository>(relaxed = true)
    private val ensure = EnsureUserProfileService(userRepository)
    private val userId = User.Id.of(UUID.fromString("11111111-1111-1111-1111-111111111111"))

    @Test
    fun `永続化がすべて失敗したら最後の例外を再送出する`() {
        every { userRepository.findById(userId) } returns null
        every { userRepository.save(any()) } throws RuntimeException("db down")

        assertFailsWith<RuntimeException> {
            ensureUserProfileResilient(ensure, userId, "Alice", maxAttempts = 3)
        }
        verify(exactly = 3) { userRepository.save(any()) }
    }

    @Test
    fun `IllegalArgumentException はリトライせず即失敗する`() {
        every { userRepository.findById(userId) } returns null

        assertFailsWith<IllegalArgumentException> {
            ensureUserProfileResilient(ensure, userId, "   ", maxAttempts = 3)
        }
        verify(exactly = 0) { userRepository.save(any()) }
    }
}

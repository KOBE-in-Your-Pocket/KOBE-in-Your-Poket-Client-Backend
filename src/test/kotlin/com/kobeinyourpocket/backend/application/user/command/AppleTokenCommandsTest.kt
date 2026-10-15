package com.kobeinyourpocket.backend.application.user.command

import com.kobeinyourpocket.backend.application.user.auth.AppleTokenException
import com.kobeinyourpocket.backend.application.user.auth.AppleTokenGateway
import com.kobeinyourpocket.backend.application.user.auth.AppleTokenGrant
import com.kobeinyourpocket.backend.application.user.auth.AuthSession
import com.kobeinyourpocket.backend.domain.user.model.PublicUser
import com.kobeinyourpocket.backend.domain.user.model.User
import com.kobeinyourpocket.backend.domain.user.repository.AppleRefreshTokenRepository
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertSame

class SignInWithAppleServiceTest {
    private val signInWithIdTokenService = mockk<SignInWithIdTokenService>()
    private val appleTokenGateway = mockk<AppleTokenGateway>()
    private val appleRefreshTokenRepository = mockk<AppleRefreshTokenRepository>()
    private val service = SignInWithAppleService(signInWithIdTokenService, appleTokenGateway, appleRefreshTokenRepository)

    private val userId = User.Id.of(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"))
    private val result =
        AuthCommandResult(
            session =
                AuthSession(
                    userId = userId,
                    accessToken = "access",
                    refreshToken = "refresh",
                    expiresIn = 3600,
                    tokenType = "bearer",
                ),
            user = PublicUser(id = userId, name = "Apple Taro"),
        )

    @BeforeTest
    fun setUp() {
        every { signInWithIdTokenService.execute("apple", "id-token", null, "nonce", "Apple Taro") } returns result
        every { appleTokenGateway.isConfigured } returns true
        every { appleTokenGateway.subjectOf("id-token") } returns "apple-sub"
        justRun { appleRefreshTokenRepository.save(any(), any()) }
    }

    private fun signIn(authorizationCode: String?) =
        service.execute(
            idToken = "id-token",
            nonce = "nonce",
            name = "Apple Taro",
            authorizationCode = authorizationCode,
        )

    @Test
    fun `authorizationCode を refresh token に交換して保存し、サインイン結果を返す`() {
        every { appleTokenGateway.exchangeAuthorizationCode("code") } returns
            AppleTokenGrant(refreshToken = "apple-refresh", subject = "apple-sub")

        val actual = signIn("code")

        assertSame(result, actual)
        // サインイン（GoTrue での ID トークン検証）が通ってから Apple へ交換しに行く。
        verifyOrder {
            signInWithIdTokenService.execute("apple", "id-token", null, "nonce", "Apple Taro")
            appleTokenGateway.exchangeAuthorizationCode("code")
            appleRefreshTokenRepository.save(userId, "apple-refresh")
        }
    }

    @Test
    fun `authorizationCode が無ければ Apple を呼ばない`() {
        signIn(null)
        signIn("  ")

        verify(exactly = 0) { appleTokenGateway.exchangeAuthorizationCode(any()) }
        verify(exactly = 0) { appleRefreshTokenRepository.save(any(), any()) }
    }

    @Test
    fun `秘密鍵が未設定なら Apple を呼ばずにサインインだけ成功させる`() {
        every { appleTokenGateway.isConfigured } returns false

        val actual = signIn("code")

        assertSame(result, actual)
        verify(exactly = 0) { appleTokenGateway.exchangeAuthorizationCode(any()) }
    }

    @Test
    fun `交換に失敗してもサインインは成功させる`() {
        every { appleTokenGateway.exchangeAuthorizationCode("code") } throws
            AppleTokenException("Apple /auth/token failed: status=400")

        val actual = signIn("code")

        assertSame(result, actual)
        verify(exactly = 0) { appleRefreshTokenRepository.save(any(), any()) }
    }

    @Test
    fun `保存に失敗してもサインインは成功させる`() {
        every { appleTokenGateway.exchangeAuthorizationCode("code") } returns
            AppleTokenGrant(refreshToken = "apple-refresh", subject = "apple-sub")
        every { appleRefreshTokenRepository.save(any(), any()) } throws IllegalStateException("db down")

        val actual = signIn("code")

        assertSame(result, actual)
    }

    @Test
    fun `交換で得た Apple ID がサインインの ID トークンと違えば保存しない`() {
        every { appleTokenGateway.exchangeAuthorizationCode("code") } returns
            AppleTokenGrant(refreshToken = "apple-refresh", subject = "other-sub")

        signIn("code")

        verify(exactly = 0) { appleRefreshTokenRepository.save(any(), any()) }
    }

    @Test
    fun `交換結果に Apple ID が無ければ保存しない`() {
        every { appleTokenGateway.exchangeAuthorizationCode("code") } returns
            AppleTokenGrant(refreshToken = "apple-refresh", subject = null)

        signIn("code")

        verify(exactly = 0) { appleRefreshTokenRepository.save(any(), any()) }
    }
}

class RevokeAppleTokenServiceTest {
    private val appleTokenGateway = mockk<AppleTokenGateway>()
    private val appleRefreshTokenRepository = mockk<AppleRefreshTokenRepository>()
    private val service = RevokeAppleTokenService(appleTokenGateway, appleRefreshTokenRepository)

    private val userId = User.Id.of(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"))

    @Test
    fun `保存済みの refresh token を失効させてから削除する`() {
        every { appleRefreshTokenRepository.findByUserId(userId) } returns "apple-refresh"
        justRun { appleTokenGateway.revokeRefreshToken("apple-refresh") }
        justRun { appleRefreshTokenRepository.deleteByUserId(userId) }

        service.execute(userId)

        verifyOrder {
            appleTokenGateway.revokeRefreshToken("apple-refresh")
            appleRefreshTokenRepository.deleteByUserId(userId)
        }
    }

    @Test
    fun `失効に失敗しても例外を投げず、保存値は削除する`() {
        every { appleRefreshTokenRepository.findByUserId(userId) } returns "apple-refresh"
        every { appleTokenGateway.revokeRefreshToken("apple-refresh") } throws
            AppleTokenException("Apple /auth/revoke failed: status=503")
        justRun { appleRefreshTokenRepository.deleteByUserId(userId) }

        service.execute(userId)

        verify(exactly = 1) { appleRefreshTokenRepository.deleteByUserId(userId) }
    }

    @Test
    fun `保存値が無ければ Apple を呼ばない`() {
        every { appleRefreshTokenRepository.findByUserId(userId) } returns null

        service.execute(userId)

        verify(exactly = 0) { appleTokenGateway.revokeRefreshToken(any()) }
        verify(exactly = 0) { appleRefreshTokenRepository.deleteByUserId(any()) }
    }
}

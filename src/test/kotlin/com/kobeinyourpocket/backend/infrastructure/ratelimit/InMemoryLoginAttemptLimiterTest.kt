package com.kobeinyourpocket.backend.infrastructure.ratelimit

import com.kobeinyourpocket.backend.application.user.auth.TooManyAuthAttemptsException
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertFailsWith

class InMemoryLoginAttemptLimiterTest {
    private val limiter =
        InMemoryLoginAttemptLimiter(
            FixedWindowRateLimiter(maxHits = 5, window = Duration.ofMinutes(15), maxKeys = 100),
        )

    private fun exhaust(
        ip: String,
        email: String,
    ) = repeat(5) { limiter.acquire(ip, email) }

    @Test
    fun `同じ IP と email で 5 回試すと 6 回目は TooManyAuthAttemptsException`() {
        exhaust("203.0.113.1", "a@example.com")

        assertFailsWith<TooManyAuthAttemptsException> { limiter.acquire("203.0.113.1", "a@example.com") }
    }

    @Test
    fun `別の IP からは同じ email で試せる（ロックアウト攻撃が成立しない）`() {
        exhaust("203.0.113.1", "a@example.com")

        limiter.acquire("198.51.100.7", "a@example.com")
    }

    @Test
    fun `同じ IP でも別の email は独立して数える`() {
        exhaust("203.0.113.1", "a@example.com")

        limiter.acquire("203.0.113.1", "b@example.com")
    }

    @Test
    fun `email の大文字小文字と前後空白は同じアカウントとして数える`() {
        exhaust("203.0.113.1", "a@example.com")

        assertFailsWith<TooManyAuthAttemptsException> { limiter.acquire("203.0.113.1", " A@Example.COM ") }
    }

    @Test
    fun `reset すると回数が消える`() {
        exhaust("203.0.113.1", "a@example.com")

        limiter.reset("203.0.113.1", "A@example.com")

        limiter.acquire("203.0.113.1", "a@example.com")
    }
}

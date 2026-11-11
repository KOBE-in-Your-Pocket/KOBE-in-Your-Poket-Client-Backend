package com.kobeinyourpocket.backend.infrastructure.ratelimit

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class FixedWindowRateLimiterTest {
    private class MutableClock(
        var now: Instant,
    ) : Clock() {
        override fun getZone(): ZoneId = ZoneId.of("UTC")

        override fun withZone(zone: ZoneId?): Clock = this

        override fun instant(): Instant = now

        fun advance(duration: Duration) {
            now = now.plus(duration)
        }
    }

    private val clock = MutableClock(Instant.parse("2026-11-11T00:00:00Z"))
    private val window = Duration.ofMinutes(15)
    private val limiter = FixedWindowRateLimiter(maxHits = 3, window = window, maxKeys = 100, clock = clock)

    @Test
    fun `上限回数までは許可し、超えたら残り時間を返す`() {
        repeat(3) { assertNull(limiter.tryAcquire("k")) }

        assertEquals(window, limiter.tryAcquire("k"))
    }

    @Test
    fun `ロックは上限に達した時点から window の間続き、経過後は再び許可する`() {
        repeat(3) { limiter.tryAcquire("k") }

        clock.advance(Duration.ofMinutes(10))
        assertEquals(Duration.ofMinutes(5), limiter.tryAcquire("k"))

        clock.advance(Duration.ofMinutes(5))
        assertNull(limiter.tryAcquire("k"))
    }

    @Test
    fun `ロック中の試行でロックは延長されない`() {
        repeat(3) { limiter.tryAcquire("k") }
        clock.advance(Duration.ofMinutes(14))
        limiter.tryAcquire("k")

        clock.advance(Duration.ofMinutes(1))
        assertNull(limiter.tryAcquire("k"))
    }

    @Test
    fun `window が過ぎたら回数は数え直す`() {
        repeat(2) { limiter.tryAcquire("k") }

        clock.advance(window)
        repeat(3) { assertNull(limiter.tryAcquire("k")) }
        assertNotNull(limiter.tryAcquire("k"))
    }

    @Test
    fun `キーごとに独立して数える`() {
        repeat(3) { limiter.tryAcquire("a") }

        assertNotNull(limiter.tryAcquire("a"))
        assertNull(limiter.tryAcquire("b"))
    }

    @Test
    fun `reset するとロック中でも許可する`() {
        repeat(3) { limiter.tryAcquire("k") }

        limiter.reset("k")

        assertNull(limiter.tryAcquire("k"))
    }
}

package com.kobeinyourpocket.backend.infrastructure.ratelimit

import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** 同じ IP × 同じスポットの閲覧を 10 分に 1 回にまとめる（#260）。時計を動かして確かめる。 */
class InMemorySpotViewDeduplicatorTest {
    private class MutableClock(
        var now: Instant,
    ) : Clock() {
        override fun getZone() = ZoneOffset.UTC

        override fun withZone(zone: java.time.ZoneId?) = this

        override fun instant() = now
    }

    private val clock = MutableClock(Instant.parse("2026-11-24T10:00:00Z"))
    private val deduplicator =
        InMemorySpotViewDeduplicator(
            FixedWindowRateLimiter(maxHits = 1, window = Duration.ofMinutes(10), maxKeys = 100, clock = clock),
        )

    private val portTower = SpotId.of("kobe-port-tower")
    private val harborland = SpotId.of("kobe-harborland")

    @Test
    fun `1 回目は数え、10 分以内の 2 回目以降はまとめる`() {
        assertTrue(deduplicator.tryCount("203.0.113.1", portTower))
        assertFalse(deduplicator.tryCount("203.0.113.1", portTower))
        clock.now = clock.now.plus(Duration.ofMinutes(9).plusSeconds(59))
        assertFalse(deduplicator.tryCount("203.0.113.1", portTower))
    }

    @Test
    fun `10 分経ったら再び数える`() {
        assertTrue(deduplicator.tryCount("203.0.113.1", portTower))
        clock.now = clock.now.plus(Duration.ofMinutes(10))

        assertTrue(deduplicator.tryCount("203.0.113.1", portTower))
    }

    @Test
    fun `別のスポットは数える`() {
        assertTrue(deduplicator.tryCount("203.0.113.1", portTower))

        assertTrue(deduplicator.tryCount("203.0.113.1", harborland))
    }

    @Test
    fun `別の IP からは数える`() {
        assertTrue(deduplicator.tryCount("203.0.113.1", portTower))

        assertTrue(deduplicator.tryCount("198.51.100.7", portTower))
    }
}

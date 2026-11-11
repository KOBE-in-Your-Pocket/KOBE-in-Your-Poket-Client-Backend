package com.kobeinyourpocket.backend.infrastructure.ratelimit

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import java.time.Clock
import java.time.Duration
import java.time.Instant

/**
 * キーごとの固定ウィンドウ回数制限（#244）。インメモリ（EC2 1 台構成のため）。
 *
 * - [window] の間に [maxHits] 回まで許可する。上限に達した時点から [window] の間はロックする
 * - 再起動でカウンタは消える（許容）
 * - IP を変えながらの大量アクセスでメモリが膨らまないよう、保持するキー数を [maxKeys] で抑える
 *
 * 判定の時刻は [clock] を使う。Caffeine の期限切れはメモリ解放のためだけに使い、
 * 期限（[window]）は判定上の寿命以上になるため判定に影響しない。
 */
class FixedWindowRateLimiter(
    private val maxHits: Int,
    private val window: Duration,
    maxKeys: Long,
    private val clock: Clock = Clock.systemUTC(),
) {
    init {
        require(maxHits >= 1) { "maxHits must be at least 1" }
        require(!window.isNegative && !window.isZero) { "window must be positive" }
    }

    private data class Bucket(
        val windowStart: Instant,
        val hits: Int,
        val blockedUntil: Instant?,
    )

    private val buckets: Cache<String, Bucket> =
        Caffeine
            .newBuilder()
            .maximumSize(maxKeys)
            .expireAfterWrite(window)
            .build()

    /**
     * 1 回分を数える。許可したら null、ロック中ならロック解除までの残り時間を返す。
     * 判定と加算は同じ compute 内で行い、同時リクエストでも上限を超えて通さない。
     */
    fun tryAcquire(key: String): Duration? {
        var retryAfter: Duration? = null
        buckets.asMap().compute(key) { _, current ->
            val now = clock.instant()
            val blockedUntil = current?.blockedUntil
            if (blockedUntil != null && now.isBefore(blockedUntil)) {
                retryAfter = Duration.between(now, blockedUntil)
                return@compute current
            }
            val base =
                if (current == null || blockedUntil != null || !now.isBefore(current.windowStart.plus(window))) {
                    Bucket(windowStart = now, hits = 0, blockedUntil = null)
                } else {
                    current
                }
            val hits = base.hits + 1
            base.copy(hits = hits, blockedUntil = if (hits >= maxHits) now.plus(window) else null)
        }
        return retryAfter
    }

    fun reset(key: String) {
        buckets.invalidate(key)
    }
}

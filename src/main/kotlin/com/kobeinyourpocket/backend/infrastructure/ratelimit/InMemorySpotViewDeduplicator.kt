package com.kobeinyourpocket.backend.infrastructure.ratelimit

import com.kobeinyourpocket.backend.application.tourism.command.SpotViewDeduplicator
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId

/**
 * [SpotViewDeduplicator] のインメモリ実装（#260）。EC2 1 台構成のため、メモリ上で十分。
 *
 * `maxHits = 1` の [FixedWindowRateLimiter] を使う。1 回目で window の間ロックされるので、
 * 「同じ閲覧者 × 同じスポットは window に 1 回」になる。判定と加算は limiter の compute 内で行うため、
 * 同時に呼ばれても 2 回数えない。再起動でカウンタが消えるのは許容する（#242）。
 */
class InMemorySpotViewDeduplicator(
    private val limiter: FixedWindowRateLimiter,
) : SpotViewDeduplicator {
    override fun tryCount(
        viewerKey: String,
        spotId: SpotId,
    ): Boolean = limiter.tryAcquire("$viewerKey|${spotId.value}") == null
}

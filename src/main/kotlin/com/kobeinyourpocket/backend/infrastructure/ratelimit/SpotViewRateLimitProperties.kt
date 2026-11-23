package com.kobeinyourpocket.backend.infrastructure.ratelimit

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * 閲覧を数える API の重複まとめとレート制限（#260 / 数え方は #242 で決定）。
 *
 * - [dedupeWindow]: 同じ IP × 同じスポットの閲覧を 1 回にまとめる時間
 * - [ip]: 利用者 IP ごとの `POST .../views` の全リクエスト。超えたら 429
 */
@ConfigurationProperties(prefix = "spot-view.rate-limit")
data class SpotViewRateLimitProperties(
    val dedupeWindow: Duration = Duration.ofMinutes(10),
    val ip: Limit = Limit(maxRequests = 60, window = Duration.ofMinutes(1)),
    /** 1 つの制限あたり保持するキーの上限（メモリ保護）。 */
    val maxTrackedKeys: Long = 100_000,
) {
    data class Limit(
        val maxRequests: Int,
        val window: Duration,
    )
}

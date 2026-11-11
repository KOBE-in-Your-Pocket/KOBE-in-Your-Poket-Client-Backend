package com.kobeinyourpocket.backend.infrastructure.ratelimit

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * ログイン・サインアップのレート制限（#244）。
 *
 * - [loginFailure]: 利用者 IP × email ごとの連続ログイン失敗
 * - [ip]: 利用者 IP ごとの login・signup の全リクエスト（CGNAT の巻き込みを避けるため緩め）
 */
@ConfigurationProperties(prefix = "auth.rate-limit")
data class AuthRateLimitProperties(
    val loginFailure: Limit = Limit(maxAttempts = 5, window = Duration.ofMinutes(15)),
    val ip: Limit = Limit(maxAttempts = 30, window = Duration.ofMinutes(15)),
    /** 1 つの制限あたり保持するキーの上限（メモリ保護）。 */
    val maxTrackedKeys: Long = 100_000,
) {
    data class Limit(
        val maxAttempts: Int,
        val window: Duration,
    )
}

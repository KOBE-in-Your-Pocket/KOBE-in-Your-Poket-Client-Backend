package com.kobeinyourpocket.backend.infrastructure.ratelimit

import com.kobeinyourpocket.backend.application.user.auth.LoginAttemptLimiter
import com.kobeinyourpocket.backend.application.user.auth.TooManyAuthAttemptsException

/**
 * [LoginAttemptLimiter] のインメモリ実装（#244）。
 *
 * email は大文字小文字・前後空白の違いで別アカウント扱いにならないよう正規化してキーにする。
 */
class InMemoryLoginAttemptLimiter(
    private val limiter: FixedWindowRateLimiter,
) : LoginAttemptLimiter {
    override fun acquire(
        clientIp: String,
        email: String,
    ) {
        limiter.tryAcquire(key(clientIp, email))?.let { throw TooManyAuthAttemptsException(it) }
    }

    override fun reset(
        clientIp: String,
        email: String,
    ) {
        limiter.reset(key(clientIp, email))
    }

    private fun key(
        clientIp: String,
        email: String,
    ): String = "$clientIp|${email.trim().lowercase()}"
}

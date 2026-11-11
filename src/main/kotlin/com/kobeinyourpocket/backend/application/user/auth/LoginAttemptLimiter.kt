package com.kobeinyourpocket.backend.application.user.auth

import java.time.Duration

/**
 * メール+パスワードログインの試行回数制限（#244）。
 *
 * キーは「利用者 IP × アカウント（email）」。email 単体では数えない
 * （第三者が被害者のアカウントをわざとロックできてしまうため）。
 * 試行は Gateway 呼び出し前に数え、ログイン成功で [reset] する。
 * 結果として「連続失敗の回数」で制限がかかる。
 */
interface LoginAttemptLimiter {
    /**
     * 試行を 1 回数える。上限に達してロック中なら [TooManyAuthAttemptsException] を投げる。
     */
    fun acquire(
        clientIp: String,
        email: String,
    )

    /** ログイン成功時に、その IP × email の試行回数を消す。 */
    fun reset(
        clientIp: String,
        email: String,
    )
}

/**
 * 認証の試行回数が上限を超えた（#244）。REST では 429 + `Retry-After` にする。
 *
 * IP 単位・IP × email 単位のどちらで止めたかは応答に出さない（アカウントの有無を推測させない）。
 */
class TooManyAuthAttemptsException(
    val retryAfter: Duration,
) : RuntimeException(MESSAGE) {
    companion object {
        const val MESSAGE = "Too many attempts. Please try again later."
    }
}

package com.kobeinyourpocket.backend.infrastructure.rest.common

import java.time.Duration

/** `Retry-After` ヘッダの秒数（#244）。端数は切り上げ、最低 1 秒。 */
fun retryAfterSeconds(retryAfter: Duration): Long {
    val seconds = retryAfter.seconds + if (retryAfter.nano > 0) 1 else 0
    return seconds.coerceAtLeast(1)
}

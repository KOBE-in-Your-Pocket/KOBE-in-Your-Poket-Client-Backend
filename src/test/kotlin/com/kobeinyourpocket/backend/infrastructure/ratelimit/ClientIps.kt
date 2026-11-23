package com.kobeinyourpocket.backend.infrastructure.ratelimit

import org.springframework.test.web.servlet.request.RequestPostProcessor
import java.util.concurrent.atomic.AtomicInteger

/**
 * 利用者 IP（`request.remoteAddr`）を指定するテスト用ヘルパー（#260）。
 *
 * 閲覧の重複まとめ・レート制限のカウンタはインメモリの Bean で、テストのロールバックでは消えず、
 * Spring のテストコンテキストを共有するテスト同士で残る。閲覧を記録するテストは [uniqueClientIp] で
 * 毎回別の IP から呼び、ほかのテストの閲覧とまとめられないようにする。
 */
object ClientIps {
    private val sequence = AtomicInteger()

    /** 呼ぶたびに別の IP（10.x.y.z）を返す。 */
    fun uniqueClientIp(): String {
        val n = sequence.incrementAndGet()
        return "10.${(n shr 16) and 0xFF}.${(n shr 8) and 0xFF}.${n and 0xFF}"
    }

    /** リクエストの送信元 IP を [ip] にする。 */
    fun fromIp(ip: String): RequestPostProcessor =
        RequestPostProcessor { request ->
            request.remoteAddr = ip
            request
        }

    /** 毎回別の IP から送る。 */
    fun fromUniqueIp(): RequestPostProcessor = fromIp(uniqueClientIp())
}

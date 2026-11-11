package com.kobeinyourpocket.backend.infrastructure.ratelimit

import com.kobeinyourpocket.backend.application.user.auth.TooManyAuthAttemptsException
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import tools.jackson.databind.json.JsonMapper
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthIpRateLimitFilterTest {
    private val objectMapper = JsonMapper.builder().build()
    private val filter =
        AuthIpRateLimitFilter(
            limiter = FixedWindowRateLimiter(maxHits = 3, window = Duration.ofMinutes(15), maxKeys = 100),
            objectMapper = objectMapper,
        )

    private fun perform(
        method: String = "POST",
        path: String = "/api/v1/auth/login",
        ip: String = "203.0.113.1",
    ): Pair<MockHttpServletResponse, MockFilterChain> {
        val request = MockHttpServletRequest(method, path).apply { remoteAddr = ip }
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()
        filter.doFilter(request, response, chain)
        return response to chain
    }

    @Test
    fun `上限までは通し、超えたら 429 と Retry-After と統一エラー本文を返す`() {
        repeat(3) {
            val (_, chain) = perform()
            assertNotNull(chain.request)
        }

        val (response, chain) = perform()

        assertNull(chain.request)
        assertEquals(429, response.status)
        assertEquals((15 * 60).toString(), response.getHeader("Retry-After"))
        val body = objectMapper.readTree(response.contentAsString)
        assertEquals(429, body["status"].asInt())
        assertEquals(TooManyAuthAttemptsException.MESSAGE, body["message"].asString())
    }

    @Test
    fun `login と signup を合わせて数える`() {
        perform(path = "/api/v1/auth/login")
        perform(path = "/api/v1/auth/signup")
        perform(path = "/api/v1/auth/login")

        val (response, _) = perform(path = "/api/v1/auth/signup")

        assertEquals(429, response.status)
    }

    @Test
    fun `別の IP は独立して数える`() {
        repeat(3) { perform(ip = "203.0.113.1") }

        val (response, chain) = perform(ip = "198.51.100.7")

        assertEquals(200, response.status)
        assertNotNull(chain.request)
    }

    @Test
    fun `対象外のパスとメソッドは数えずに通す`() {
        repeat(5) {
            val (_, chain) = perform(path = "/api/v1/auth/google")
            assertNotNull(chain.request)
        }
        repeat(5) {
            val (_, chain) = perform(method = "GET")
            assertNotNull(chain.request)
        }

        val (_, chain) = perform()
        assertTrue(chain.request != null)
    }
}

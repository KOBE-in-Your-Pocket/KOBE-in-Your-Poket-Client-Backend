package com.kobeinyourpocket.backend.infrastructure.ratelimit

import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import tools.jackson.databind.json.JsonMapper
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** 閲覧を数える API の IP ごとのレート制限（#260）。 */
class SpotViewIpRateLimitFilterTest {
    private val objectMapper = JsonMapper.builder().build()
    private val filter =
        SpotViewIpRateLimitFilter(
            limiter = FixedWindowRateLimiter(maxHits = 3, window = Duration.ofMinutes(1), maxKeys = 100),
            objectMapper = objectMapper,
        )

    private fun perform(
        method: String = "POST",
        path: String = "/api/v1/tourism/spots/kobe-port-tower/views",
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
        assertEquals("60", response.getHeader("Retry-After"))
        val body = objectMapper.readTree(response.contentAsString)
        assertEquals(429, body["status"].asInt())
        assertEquals(SpotViewIpRateLimitFilter.MESSAGE, body["message"].asString())
    }

    @Test
    fun `スポットが違っても同じ IP なら合わせて数える`() {
        perform(path = "/api/v1/tourism/spots/a/views")
        perform(path = "/api/v1/tourism/spots/b/views")
        perform(path = "/api/v1/tourism/spots/c/views")

        val (response, _) = perform(path = "/api/v1/tourism/spots/d/views")

        assertEquals(429, response.status)
    }

    @Test
    fun `別の IP は独立して数える`() {
        repeat(3) { perform(ip = "203.0.113.1") }

        val (_, chain) = perform(ip = "198.51.100.7")

        assertNotNull(chain.request)
    }

    @Test
    fun `閲覧の記録以外は数えない`() {
        repeat(5) {
            val (_, chain) = perform(method = "GET", path = "/api/v1/tourism/spots/kobe-port-tower")
            assertNotNull(chain.request)
        }
        repeat(5) {
            val (_, chain) = perform(path = "/api/v1/tourism/spots/kobe-port-tower/reviews")
            assertNotNull(chain.request)
        }
        repeat(5) {
            val (_, chain) = perform(method = "GET", path = "/api/v1/tourism/spots/kobe-port-tower/views")
            assertNotNull(chain.request)
        }

        val (_, chain) = perform()

        assertNotNull(chain.request)
    }
}

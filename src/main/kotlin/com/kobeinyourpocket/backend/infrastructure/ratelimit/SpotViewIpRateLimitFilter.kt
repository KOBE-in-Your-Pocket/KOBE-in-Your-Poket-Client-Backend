package com.kobeinyourpocket.backend.infrastructure.ratelimit

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpMethod
import org.springframework.web.filter.OncePerRequestFilter
import tools.jackson.databind.ObjectMapper

/**
 * `POST /api/v1/tourism/spots/{spotId}/views` を利用者 IP ごとに数え、上限を超えたら 429 を返す（#260 / #242）。
 *
 * いたずらで大量に送られたときの保険。普通の使い方では届かない回数にする（既定は 1 分に 60 回）。
 * 存在しないスポットへの呼び出しも数える（DB に問い合わせる前に止めるため）。
 * 利用者 IP は `request.remoteAddr`（[AuthIpRateLimitFilter] と同じ）。
 */
class SpotViewIpRateLimitFilter(
    private val limiter: FixedWindowRateLimiter,
    private val objectMapper: ObjectMapper,
) : OncePerRequestFilter() {
    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        request.method != HttpMethod.POST.name() || !TARGET_PATH.matches(request.requestURI)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val retryAfter = limiter.tryAcquire(request.remoteAddr)
        if (retryAfter == null) {
            filterChain.doFilter(request, response)
            return
        }
        writeTooManyRequests(response, objectMapper, retryAfter, MESSAGE)
    }

    companion object {
        /** Servlet の URL パターンは途中の `*` を書けないため、登録は前方一致にしてここで絞る。 */
        const val URL_PATTERN = "/api/v1/tourism/spots/*"

        val TARGET_PATH = Regex("^/api/v1/tourism/spots/[^/]+/views$")

        const val MESSAGE = "Too many requests. Please try again later."
    }
}

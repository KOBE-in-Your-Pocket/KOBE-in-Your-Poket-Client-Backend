package com.kobeinyourpocket.backend.infrastructure.ratelimit

import com.kobeinyourpocket.backend.application.user.auth.TooManyAuthAttemptsException
import com.kobeinyourpocket.backend.infrastructure.rest.common.ApiErrorResponse
import com.kobeinyourpocket.backend.infrastructure.rest.common.retryAfterSeconds
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.filter.OncePerRequestFilter
import tools.jackson.databind.ObjectMapper

/**
 * `POST /api/v1/auth/login|signup` を利用者 IP ごとに数え、上限を超えたら 429 を返す（#244）。
 *
 * 1 つの IP から email を変えながら試す攻撃と、大量の signup を止める保険。
 * 利用者 IP は `request.remoteAddr`。本番は Caddy の `X-Forwarded-For` を
 * Tomcat（`server.forward-headers-strategy: native`）が解釈した実 IP になる。
 *
 * 応答本文は [com.kobeinyourpocket.backend.infrastructure.rest.common.GlobalExceptionHandler] の
 * [TooManyAuthAttemptsException] と同じにし、どちらの制限で止めたかを区別させない。
 */
class AuthIpRateLimitFilter(
    private val limiter: FixedWindowRateLimiter,
    private val objectMapper: ObjectMapper,
) : OncePerRequestFilter() {
    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        request.method != HttpMethod.POST.name() || request.requestURI !in TARGET_PATHS

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
        val status = HttpStatus.TOO_MANY_REQUESTS
        response.status = status.value()
        response.setHeader(HttpHeaders.RETRY_AFTER, retryAfterSeconds(retryAfter).toString())
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        objectMapper.writeValue(
            response.writer,
            ApiErrorResponse(
                status = status.value(),
                error = status.reasonPhrase,
                message = TooManyAuthAttemptsException.MESSAGE,
            ),
        )
    }

    companion object {
        val TARGET_PATHS = listOf("/api/v1/auth/login", "/api/v1/auth/signup")
    }
}

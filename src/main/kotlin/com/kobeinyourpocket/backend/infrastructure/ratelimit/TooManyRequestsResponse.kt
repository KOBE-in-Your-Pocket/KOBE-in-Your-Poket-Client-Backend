package com.kobeinyourpocket.backend.infrastructure.ratelimit

import com.kobeinyourpocket.backend.infrastructure.rest.common.ApiErrorResponse
import com.kobeinyourpocket.backend.infrastructure.rest.common.retryAfterSeconds
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import tools.jackson.databind.ObjectMapper
import java.time.Duration

/**
 * Filter から 429 を返す（`Retry-After` 付き・本文は統一エラー JSON）。
 * Filter は `@RestControllerAdvice` を通らないため、ここで直接書く（#244 / #260 で共通）。
 */
fun writeTooManyRequests(
    response: HttpServletResponse,
    objectMapper: ObjectMapper,
    retryAfter: Duration,
    message: String,
) {
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
            message = message,
        ),
    )
}

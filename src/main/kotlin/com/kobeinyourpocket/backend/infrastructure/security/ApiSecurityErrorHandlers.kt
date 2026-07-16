package com.kobeinyourpocket.backend.infrastructure.security

import com.kobeinyourpocket.backend.infrastructure.rest.common.ApiErrorResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

/**
 * Resource Server 既定の 401/403 レスポンスを、REST API の統一エラー形式
 * （[ApiErrorResponse]・§3.3 / #24）で差し替える（#90）。
 *
 * Security フィルタ層で拒否されるため `GlobalExceptionHandler` には届かない。
 * ここで同じ JSON 形式を直接書き出す。
 */
@Component
class ApiAuthenticationEntryPoint(
    private val objectMapper: ObjectMapper,
) : AuthenticationEntryPoint {
    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
        response.writeApiError(objectMapper, HttpStatus.UNAUTHORIZED, "Authentication is required")
    }
}

/** 認証済みだが権限不足（運営ロールなし等）の 403 を統一エラー形式で返す（#90）。 */
@Component
class ApiAccessDeniedHandler(
    private val objectMapper: ObjectMapper,
) : AccessDeniedHandler {
    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException,
    ) {
        response.writeApiError(objectMapper, HttpStatus.FORBIDDEN, "Access is denied")
    }
}

private fun HttpServletResponse.writeApiError(
    objectMapper: ObjectMapper,
    status: HttpStatus,
    message: String,
) {
    this.status = status.value()
    contentType = MediaType.APPLICATION_JSON_VALUE
    characterEncoding = Charsets.UTF_8.name()
    objectMapper.writeValue(
        writer,
        ApiErrorResponse(
            status = status.value(),
            error = status.reasonPhrase,
            message = message,
        ),
    )
}

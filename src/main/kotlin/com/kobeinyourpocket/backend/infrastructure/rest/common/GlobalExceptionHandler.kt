package com.kobeinyourpocket.backend.infrastructure.rest.common

import com.kobeinyourpocket.backend.application.tourism.query.SpotNotFoundException
import com.kobeinyourpocket.backend.application.user.auth.AuthGatewayException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/** バリデーション・不正リクエストの統一エラー応答（§3.3 / #24）。 */
@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(SpotNotFoundException::class)
    fun handleSpotNotFound(ex: SpotNotFoundException): ResponseEntity<ApiErrorResponse> = notFound(message = ex.message ?: "Spot not found")

    @ExceptionHandler(AuthGatewayException::class)
    fun handleAuthGateway(ex: AuthGatewayException): ResponseEntity<ApiErrorResponse> {
        val status = HttpStatus.resolve(ex.status) ?: HttpStatus.BAD_GATEWAY
        val safeStatus =
            when {
                status.is4xxClientError -> status
                status == HttpStatus.TOO_MANY_REQUESTS -> status
                else -> HttpStatus.BAD_GATEWAY
            }
        return ResponseEntity
            .status(safeStatus)
            .body(
                ApiErrorResponse(
                    status = safeStatus.value(),
                    error = safeStatus.reasonPhrase,
                    message = ex.message ?: "Authentication failed",
                ),
            )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ApiErrorResponse> =
        badRequest(
            message = "Validation failed",
            violations =
                ex.bindingResult.fieldErrors.map { error ->
                    ApiErrorResponse.FieldViolation(
                        field = error.field,
                        message = error.defaultMessage ?: "invalid",
                    )
                },
        )

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadableBody(ex: HttpMessageNotReadableException): ResponseEntity<ApiErrorResponse> =
        badRequest(message = ex.mostSpecificCause.message ?: "Invalid request body")

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ResponseEntity<ApiErrorResponse> =
        badRequest(message = ex.message ?: "Invalid request")

    private fun badRequest(
        message: String,
        violations: List<ApiErrorResponse.FieldViolation> = emptyList(),
    ): ResponseEntity<ApiErrorResponse> =
        ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ApiErrorResponse(
                    status = HttpStatus.BAD_REQUEST.value(),
                    error = HttpStatus.BAD_REQUEST.reasonPhrase,
                    message = message,
                    violations = violations,
                ),
            )

    private fun notFound(message: String): ResponseEntity<ApiErrorResponse> =
        ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(
                ApiErrorResponse(
                    status = HttpStatus.NOT_FOUND.value(),
                    error = HttpStatus.NOT_FOUND.reasonPhrase,
                    message = message,
                ),
            )
}

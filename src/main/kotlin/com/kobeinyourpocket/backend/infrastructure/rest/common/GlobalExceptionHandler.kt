package com.kobeinyourpocket.backend.infrastructure.rest.common

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/** バリデーション・不正リクエストの統一エラー応答（§3.3 / #24）。 */
@RestControllerAdvice
class GlobalExceptionHandler {
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
}

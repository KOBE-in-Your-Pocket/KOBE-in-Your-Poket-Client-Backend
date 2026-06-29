package com.kobeinyourpocket.backend.infrastructure.rest.common

/** REST API の統一エラーレスポンス（§3.3 / #24）。 */
data class ApiErrorResponse(
    val status: Int,
    val error: String,
    val message: String,
    val violations: List<FieldViolation> = emptyList(),
) {
    data class FieldViolation(
        val field: String,
        val message: String,
    )
}

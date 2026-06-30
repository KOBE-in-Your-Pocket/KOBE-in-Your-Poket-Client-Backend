package com.kobeinyourpocket.backend.infrastructure.rest.tourism

import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

/** `POST /api/v1/tourism/spots/{spotId}/reviews` のリクエストボディ（§8）。 */
data class ReviewRequest(
    @field:Min(1)
    @field:Max(5)
    val rating: Int,
    @field:NotBlank
    val comment: String,
    @field:NotNull
    @field:Valid
    val author: AuthorBody,
    @field:NotBlank
    val language: String,
) {
    data class AuthorBody(
        @field:NotBlank
        val name: String,
        val iconUrl: String? = null,
    )
}

package com.kobeinyourpocket.backend.infrastructure.rest.tourism

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

/** `PUT /api/v1/tourism/spots/{spotId}/reviews/{reviewId}` のリクエストボディ（§8）。 */
data class ReviewUpdateRequest(
    @field:Min(1)
    @field:Max(5)
    val rating: Int,
    @field:NotBlank
    val comment: String,
)

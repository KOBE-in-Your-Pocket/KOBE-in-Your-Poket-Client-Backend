package com.kobeinyourpocket.backend.infrastructure.rest.tourism

import com.fasterxml.jackson.annotation.JsonInclude
import com.kobeinyourpocket.backend.application.tourism.query.SpotView

/**
 * `GET /api/v1/tourism/spots` のレスポンス要素（モックの `fetchSpots` 返却形に準拠 / §8）。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class SpotResponse(
    val id: String,
    val name: String,
    val genre: String,
    val description: String,
    val coordinates: CoordinatesResponse,
    val businessHours: String,
    val category: CategoryResponse,
    val media: MediaResponse,
    val rating: RatingResponse?,
) {
    data class CoordinatesResponse(
        val latitude: Double,
        val longitude: Double,
    )

    data class CategoryResponse(
        val label: String,
    )

    data class MediaResponse(
        val imageUrl: String,
    )

    data class RatingResponse(
        val value: Double,
    )

    companion object {
        fun from(view: SpotView): SpotResponse =
            SpotResponse(
                id = view.id,
                name = view.name,
                genre = view.genre,
                description = view.description,
                coordinates = CoordinatesResponse(latitude = view.latitude, longitude = view.longitude),
                businessHours = view.businessHours,
                category = CategoryResponse(label = view.categoryLabel),
                media = MediaResponse(imageUrl = view.imageUrl),
                rating = view.rating?.let { RatingResponse(value = it) },
            )
    }
}

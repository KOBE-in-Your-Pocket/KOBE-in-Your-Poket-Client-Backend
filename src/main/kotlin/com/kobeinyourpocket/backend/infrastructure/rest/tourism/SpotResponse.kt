package com.kobeinyourpocket.backend.infrastructure.rest.tourism

import com.fasterxml.jackson.annotation.JsonInclude
import com.kobeinyourpocket.backend.application.tourism.query.SpotView
import com.kobeinyourpocket.backend.domain.tourism.localization.Language
import com.kobeinyourpocket.backend.domain.tourism.spot.model.SpotWithLocalizations

/**
 * `GET /api/v1/tourism/spots` および `POST /api/v1/tourism/spots` のレスポンス（モックの `Spot` 形 / §8）。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class SpotResponse(
    val id: String,
    val name: String,
    val genre: String,
    val description: String,
    val coordinates: CoordinatesResponse,
    val address: String,
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
                address = view.address,
                businessHours = view.businessHours,
                category = CategoryResponse(label = view.categoryLabel),
                media = MediaResponse(imageUrl = view.imageUrl),
                rating = view.rating?.let { RatingResponse(value = it) },
            )

        /** POST 201 用。要求言語未指定時は en（[Language.DEFAULT]）で解決した Spot 形を返す。 */
        fun fromRegistered(
            saved: SpotWithLocalizations,
            language: Language = Language.DEFAULT,
        ): SpotResponse {
            val localization = saved.localizations.resolve(language)
            val spot = saved.spot
            return SpotResponse(
                id = spot.id.value,
                name = localization.name,
                genre = spot.genre.value,
                description = localization.description,
                coordinates =
                    CoordinatesResponse(
                        latitude = spot.coordinates.latitude,
                        longitude = spot.coordinates.longitude,
                    ),
                address = localization.address,
                businessHours = localization.businessHours,
                category = CategoryResponse(label = localization.categoryLabel),
                media = MediaResponse(imageUrl = spot.media.imageUrl),
                rating = spot.rating?.value?.let { RatingResponse(value = it) },
            )
        }
    }
}

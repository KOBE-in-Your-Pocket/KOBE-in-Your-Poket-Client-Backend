package com.kobeinyourpocket.backend.infrastructure.web.tourism

import com.fasterxml.jackson.annotation.JsonInclude
import com.kobeinyourpocket.backend.application.tourism.LocalizedSpot

/**
 * `GET /api/v1/tourism/spots` のレスポンス要素（モックの `fetchSpots` 返却形に準拠 / §8）。
 *
 * フロントが `mock-spots.ts` を実 fetch へ差し替えるだけで済むよう、フィールド名・ネスト構造を
 * Client `domain/spot.ts` の解決済み `Spot` に合わせる。`rating` はレビュー未実装時に欠落させる
 * （Client `domain/spot.ts` の optional `rating?` と整合）。
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
        /** application の解決済み出力（base [LocalizedSpot.spot] + 単一言語 [LocalizedSpot.localization]）をモック互換 JSON 形へ変換する。 */
        fun from(localized: LocalizedSpot): SpotResponse {
            val spot = localized.spot
            val localization = localized.localization
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
                businessHours = localization.businessHours,
                category = CategoryResponse(label = localization.categoryLabel),
                media = MediaResponse(imageUrl = spot.media.imageUrl),
                rating = spot.rating?.let { RatingResponse(value = it.value) },
            )
        }
    }
}

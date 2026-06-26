package com.kobeinyourpocket.backend.domain.tourism.model

/**
 * 観光スポット集約（言語非依存部分）。
 *
 * 言語依存フィールド（name / description / businessHours / category）は
 * #17 SpotLocalization で扱う。API 返却形への合成も application 層で行う。
 *
 * Client `Spot` のうち id / genre / coordinates / media / rating? に相当する。
 */
data class Spot(
    val id: SpotId,
    val genre: Genre,
    val coordinates: Coordinates,
    val media: SpotMedia,
    val rating: SpotRating? = null,
) {
    companion object {
        fun create(
            id: SpotId,
            genre: Genre,
            coordinates: Coordinates,
            media: SpotMedia,
            rating: SpotRating? = null,
        ): Spot =
            Spot(
                id = id,
                genre = genre,
                coordinates = coordinates,
                media = media,
                rating = rating,
            )
    }
}

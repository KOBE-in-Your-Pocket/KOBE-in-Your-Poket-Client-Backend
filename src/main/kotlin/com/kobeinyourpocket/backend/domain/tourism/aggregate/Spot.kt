package com.kobeinyourpocket.backend.domain.tourism.aggregate

import com.kobeinyourpocket.backend.domain.tourism.vo.Coordinates
import com.kobeinyourpocket.backend.domain.tourism.vo.Genre
import com.kobeinyourpocket.backend.domain.tourism.vo.SpotId
import com.kobeinyourpocket.backend.domain.tourism.vo.SpotMedia
import com.kobeinyourpocket.backend.domain.tourism.vo.SpotRating

/**
 * 観光スポット集約ルート（Entity・言語非依存部分）。
 *
 * `vo` パッケージの Value Object をコンポジションで保持する。
 * 同一 [SpotId] により識別され、中身の更新は同じ Spot として扱う。
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

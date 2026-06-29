package com.kobeinyourpocket.backend.infrastructure.rest.tourism

import com.kobeinyourpocket.backend.domain.tourism.vo.Language
import com.kobeinyourpocket.backend.domain.tourism.vo.SpotLocalization
import com.kobeinyourpocket.backend.domain.tourism.vo.SpotLocalizations

/** `POST /api/v1/tourism/spots` のリクエストボディ（§8）。 */
data class RegisterSpotRequest(
    val genre: String,
    val coordinates: CoordinatesBody,
    val imageUrl: String,
    val localizations: Map<String, LocalizationBody>,
) {
    data class CoordinatesBody(
        val latitude: Double,
        val longitude: Double,
    )

    data class LocalizationBody(
        val name: String,
        val categoryLabel: String,
        val description: String,
        val businessHours: String,
    )

    fun toLocalizations(): SpotLocalizations {
        val byLanguage =
            localizations
                .mapNotNull { (code, body) ->
                    Language.of(code)?.let { language ->
                        language to
                            SpotLocalization(
                                name = body.name,
                                categoryLabel = body.categoryLabel,
                                description = body.description,
                                businessHours = body.businessHours,
                            )
                    }
                }.toMap()
        return SpotLocalizations.of(byLanguage)
    }
}

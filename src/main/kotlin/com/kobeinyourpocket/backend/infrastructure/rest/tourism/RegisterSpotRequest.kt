package com.kobeinyourpocket.backend.infrastructure.rest.tourism

import com.kobeinyourpocket.backend.domain.tourism.vo.Language
import com.kobeinyourpocket.backend.domain.tourism.vo.SpotLocalization
import com.kobeinyourpocket.backend.domain.tourism.vo.SpotLocalizations
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull

/** `POST /api/v1/tourism/spots` のリクエストボディ（§8）。 */
data class RegisterSpotRequest(
    @field:NotBlank
    val genre: String,
    @field:NotNull
    @field:Valid
    val coordinates: CoordinatesBody,
    @field:NotBlank
    val imageUrl: String,
    @field:NotEmpty
    val localizations: Map<String, LocalizationBody>,
) {
    data class CoordinatesBody(
        @field:NotNull
        val latitude: Double,
        @field:NotNull
        val longitude: Double,
    )

    data class LocalizationBody(
        @field:NotBlank
        val name: String,
        @field:NotBlank
        val categoryLabel: String,
        @field:NotBlank
        val description: String,
        @field:NotBlank
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

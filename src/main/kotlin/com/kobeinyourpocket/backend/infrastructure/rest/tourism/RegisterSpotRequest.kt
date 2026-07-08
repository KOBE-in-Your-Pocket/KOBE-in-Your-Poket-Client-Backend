package com.kobeinyourpocket.backend.infrastructure.rest.tourism

import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotLocalization
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotLocalizations
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull

/**
 * `POST /api/v1/tourism/spots` のリクエストボディ（§8）。
 *
 * `localizations` はフォールバックに依存しない運用（要件定義 D1 / #84）のため、
 * ja/en/zh/ko の対応言語ちょうど4件を必須とする（[toLocalizations] で検証）。
 */
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
        @field:NotBlank
        val address: String,
    )

    fun toLocalizations(): SpotLocalizations {
        val requiredCodes = Language.entries.map { it.code }.toSet()
        require(localizations.keys == requiredCodes) {
            "localizations must contain exactly the supported languages ${requiredCodes.sorted()}, got ${localizations.keys.sorted()}"
        }
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
                                address = body.address,
                            )
                    }
                }.toMap()
        return SpotLocalizations.of(byLanguage)
    }
}

package com.kobeinyourpocket.backend.infrastructure.rest.evacuation

import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterLocalization
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterLocalizations
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

/**
 * `POST /api/v1/evacuation/shelters` のリクエストボディ。
 *
 * `type` は Client `ShelterType` と同値の `emergency|designated|both`（#162）。
 * `capacity` / `externalUrl` は任意（出典データに無いことが多く、運営が分かる範囲で埋める）。
 *
 * `localizations` は ja/en/zh/ko の 4 件必須。件数の検証は
 * [com.kobeinyourpocket.backend.application.evacuation.command.RegisterShelterService] が持つ
 * （HTTP 以外の入口からも同じ規則を通すため）。ここは JSON → ドメイン VO の変換だけを行う。
 */
data class RegisterShelterRequest(
    @field:NotNull
    @field:Valid
    val coordinates: CoordinatesBody,
    @field:NotBlank
    val type: String,
    @field:NotBlank
    val facilityCategory: String,
    @field:NotBlank
    val imageUrl: String,
    @field:NotNull
    val accessible: Boolean?,
    @field:NotEmpty
    val localizations: Map<String, LocalizationBody>,
    @field:Positive
    val capacity: Int? = null,
    val externalUrl: String? = null,
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
        val address: String,
    )

    /**
     * 言語コードをドメインの [Language] へ解決する。未対応コードは 400 にする
     * （黙って捨てると「4 件送ったのに 3 件しか入らない」状態になり、原因が分かりにくい）。
     */
    fun toLocalizations(): ShelterLocalizations {
        val byLanguage =
            localizations.map { (code, body) ->
                val language = requireNotNull(Language.of(code)) { "unsupported language code: '$code'" }
                language to ShelterLocalization(name = body.name, address = body.address)
            }
        require(byLanguage.size == byLanguage.toMap().size) { "duplicate language codes in localizations" }
        return ShelterLocalizations.of(byLanguage.toMap())
    }
}

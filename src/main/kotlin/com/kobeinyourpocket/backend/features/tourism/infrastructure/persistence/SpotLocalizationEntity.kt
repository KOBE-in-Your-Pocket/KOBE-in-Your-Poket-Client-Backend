package com.kobeinyourpocket.backend.features.tourism.infrastructure.persistence

import com.kobeinyourpocket.backend.features.tourism.domain.vo.Language
import com.kobeinyourpocket.backend.features.tourism.domain.vo.SpotId
import com.kobeinyourpocket.backend.features.tourism.domain.vo.SpotLocalization
import com.kobeinyourpocket.backend.features.tourism.domain.vo.SpotLocalizations
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.io.Serializable

/** `spot_localization` の複合主キー (spot_id, language)。 */
@Embeddable
data class SpotLocalizationId(
    @Column(name = "spot_id")
    val spotId: String,
    @Column(name = "language")
    val language: String,
) : Serializable

/** DB `spot_localization`（言語別ローカライズ 1 行）。 */
@Entity
@Table(name = "spot_localization")
class SpotLocalizationEntity(
    @EmbeddedId
    var id: SpotLocalizationId,
    @Column(name = "name", nullable = false)
    var name: String,
    @Column(name = "category_label", nullable = false)
    var categoryLabel: String,
    @Column(name = "description", nullable = false)
    var description: String,
    @Column(name = "business_hours", nullable = false)
    var businessHours: String,
) {
    companion object {
        fun fromDomain(
            spotId: SpotId,
            language: Language,
            localization: SpotLocalization,
        ): SpotLocalizationEntity =
            SpotLocalizationEntity(
                id = SpotLocalizationId(spotId = spotId.value, language = language.code),
                name = localization.name,
                categoryLabel = localization.categoryLabel,
                description = localization.description,
                businessHours = localization.businessHours,
            )
    }
}

/** 同一 Spot に属する行を [SpotLocalizations] へ復元する（未知の言語コードは永続化データ不整合として失敗）。 */
fun List<SpotLocalizationEntity>.toDomainLocalizations(): SpotLocalizations =
    SpotLocalizations.of(
        associate { entity ->
            val language =
                Language.of(entity.id.language)
                    ?: error("Unknown language code in spot_localization: '${entity.id.language}'")
            language to
                SpotLocalization(
                    name = entity.name,
                    categoryLabel = entity.categoryLabel,
                    description = entity.description,
                    businessHours = entity.businessHours,
                )
        },
    )

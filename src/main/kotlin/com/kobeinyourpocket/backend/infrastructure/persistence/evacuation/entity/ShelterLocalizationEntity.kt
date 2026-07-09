package com.kobeinyourpocket.backend.infrastructure.persistence.evacuation.entity

import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.model.EvacuationShelter
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterLocalization
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterLocalizations
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.io.Serializable

/** `shelter_localization` の複合主キー (shelter_id, language)。 */
@Embeddable
data class ShelterLocalizationId(
    @Column(name = "shelter_id")
    val shelterId: String,
    @Column(name = "language")
    val language: String,
) : Serializable

/** DB `shelter_localization`（言語別ローカライズ 1 行）。 */
@Entity
@Table(name = "shelter_localization")
class ShelterLocalizationEntity(
    @EmbeddedId
    var id: ShelterLocalizationId,
    @Column(name = "name", nullable = false)
    var name: String,
    @Column(name = "address", nullable = false)
    var address: String,
) {
    companion object {
        fun fromDomain(
            shelterId: EvacuationShelter.Id,
            language: Language,
            localization: ShelterLocalization,
        ): ShelterLocalizationEntity =
            ShelterLocalizationEntity(
                id = ShelterLocalizationId(shelterId = shelterId.value, language = language.code),
                name = localization.name,
                address = localization.address,
            )
    }
}

/** 同一 Shelter に属する行を [ShelterLocalizations] へ復元する（未知の言語コードは永続化データ不整合として失敗）。 */
fun List<ShelterLocalizationEntity>.toDomainLocalizations(): ShelterLocalizations =
    ShelterLocalizations.of(
        associate { entity ->
            val language =
                Language.of(entity.id.language)
                    ?: error("Unknown language code in shelter_localization: '${entity.id.language}'")
            language to
                ShelterLocalization(
                    name = entity.name,
                    address = entity.address,
                )
        },
    )

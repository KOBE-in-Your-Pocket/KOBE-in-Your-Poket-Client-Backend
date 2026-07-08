package com.kobeinyourpocket.backend.infrastructure.persistence.manner.entity

import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.domain.manner.manneritem.model.MannerItem
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerLocalization
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerLocalizations
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.io.Serializable

/** `manner_item_localization` の複合主キー (manner_item_id, language)。 */
@Embeddable
data class MannerItemLocalizationId(
    @Column(name = "manner_item_id")
    val mannerItemId: String,
    @Column(name = "language")
    val language: String,
) : Serializable

/** DB `manner_item_localization`（言語別ローカライズ 1 行）。 */
@Entity
@Table(name = "manner_item_localization")
class MannerItemLocalizationEntity(
    @EmbeddedId
    var id: MannerItemLocalizationId,
    @Column(name = "title", nullable = false)
    var title: String,
    @Column(name = "description", nullable = false)
    var description: String,
) {
    companion object {
        fun fromDomain(
            mannerItemId: MannerItem.Id,
            language: Language,
            localization: MannerLocalization,
        ): MannerItemLocalizationEntity =
            MannerItemLocalizationEntity(
                id = MannerItemLocalizationId(mannerItemId = mannerItemId.value, language = language.code),
                title = localization.title,
                description = localization.description,
            )
    }
}

/** 同一 MannerItem に属する行を [MannerLocalizations] へ復元する（未知の言語コードは永続化データ不整合として失敗）。 */
fun List<MannerItemLocalizationEntity>.toDomainLocalizations(): MannerLocalizations =
    MannerLocalizations.of(
        associate { entity ->
            val language =
                Language.of(entity.id.language)
                    ?: error("Unknown language code in manner_item_localization: '${entity.id.language}'")
            language to
                MannerLocalization(
                    title = entity.title,
                    description = entity.description,
                )
        },
    )

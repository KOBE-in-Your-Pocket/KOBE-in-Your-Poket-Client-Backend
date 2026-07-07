package com.kobeinyourpocket.backend.domain.manner.manneritem.model

import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerIcon
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerItemId
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerKind
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerScope
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.RelatedSpotId

/**
 * [エンティティ] マナー項目（言語非依存部分）。
 *
 * `vo` パッケージの Value Object をコンポジションで保持する。同一 [MannerItemId] により識別される。
 * [relatedSpotIds] は Tourism の `Spot.id` への ID 参照のみで、直接結合はしない（要件定義 M-2）。
 * 言語依存フィールド（title / description）は [com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerLocalization] で扱う。
 *
 * Client `MannerItem` のうち id / icon / kind / scope / relatedSpotIds に相当する。
 */
data class MannerItem(
    val id: MannerItemId,
    val icon: MannerIcon,
    val kind: MannerKind,
    val scope: MannerScope,
    val relatedSpotIds: List<RelatedSpotId>,
) {
    companion object {
        fun create(
            id: MannerItemId,
            icon: MannerIcon,
            kind: MannerKind,
            scope: MannerScope,
            relatedSpotIds: List<RelatedSpotId> = emptyList(),
        ): MannerItem =
            MannerItem(
                id = id,
                icon = icon,
                kind = kind,
                scope = scope,
                relatedSpotIds = relatedSpotIds.toList(),
            )
    }
}

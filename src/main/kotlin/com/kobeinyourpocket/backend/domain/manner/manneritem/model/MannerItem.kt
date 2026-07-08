package com.kobeinyourpocket.backend.domain.manner.manneritem.model

import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerIcon
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerKind
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerLocalizations
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerScope
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.RelatedSpotId

/**
 * マナー項目エンティティ（集約ルート）。
 *
 * 「訪問者へのマナー・ルール啓発 1 件」を表す。同一性は [Id] が担い、内容が更新されても同じ MannerItem として続く。
 * 全言語のローカライズを [localizations] として所有し、集約の整合境界に含める。
 * [relatedSpotIds] は Tourism の Spot.id への ID 参照のみで、直接結合はしない（要件定義 M-2）。
 */
data class MannerItem(
    val id: Id,
    val icon: MannerIcon,
    val kind: MannerKind,
    val scope: MannerScope,
    val relatedSpotIds: List<RelatedSpotId>,
    val localizations: MannerLocalizations,
) {
    /**
     * マナー項目の識別子（値オブジェクト）。
     *
     * エンティティの同一性そのものであり、エンティティを構成するため本ファイルに同居させる。
     * [Companion.of] が生成入口。
     */
    @JvmInline
    value class Id private constructor(
        val value: String,
    ) {
        init {
            require(value.isNotBlank()) { "MannerItem.Id must not be blank" }
            require(value.length <= MAX_LENGTH) {
                "MannerItem.Id must be at most $MAX_LENGTH characters, got ${value.length}"
            }
        }

        override fun toString(): String = value

        companion object {
            private const val MAX_LENGTH = 128

            fun of(value: String): Id = Id(value.trim())
        }
    }

    companion object {
        fun create(
            id: Id,
            icon: MannerIcon,
            kind: MannerKind,
            scope: MannerScope,
            localizations: MannerLocalizations,
            relatedSpotIds: List<RelatedSpotId> = emptyList(),
        ): MannerItem =
            MannerItem(
                id = id,
                icon = icon,
                kind = kind,
                scope = scope,
                relatedSpotIds = relatedSpotIds.toList(),
                localizations = localizations,
            )
    }
}

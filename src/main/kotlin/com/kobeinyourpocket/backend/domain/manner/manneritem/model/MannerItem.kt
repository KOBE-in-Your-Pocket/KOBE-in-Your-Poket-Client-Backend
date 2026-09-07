package com.kobeinyourpocket.backend.domain.manner.manneritem.model

import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerIcon
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerIconUrl
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
 *
 * アイコンは識別キー [icon] と画像 URL [iconUrl] の二経路を持つ。**どちらか一方は必ず要る**。
 * 両方欠けると Client がアイコンを描けない。既存項目はキーのみ、運営が管理画面から作る
 * 項目は URL のみを持つのが通常で、両方を持つ場合は URL を優先する（表示解決は Client）。
 */
data class MannerItem(
    val id: Id,
    val icon: MannerIcon?,
    val iconUrl: MannerIconUrl?,
    val kind: MannerKind,
    val scope: MannerScope,
    val relatedSpotIds: List<RelatedSpotId>,
    val localizations: MannerLocalizations,
) {
    init {
        require(icon != null || iconUrl != null) {
            "MannerItem must have either an icon key or an icon URL"
        }
    }

    /**
     * 内容を差し替えた新しい状態を返す。[id] は変えない。
     *
     * Client の項目詳細（`/manner/[id]`）の遷移先であり、変えると既存のリンクが切れる。
     * 名前を大きく変えたい場合は作り直して旧項目を削除する運用になる。
     */
    fun update(
        icon: MannerIcon?,
        iconUrl: MannerIconUrl?,
        kind: MannerKind,
        scope: MannerScope,
        relatedSpotIds: List<RelatedSpotId>,
        localizations: MannerLocalizations,
    ): MannerItem =
        copy(
            icon = icon,
            iconUrl = iconUrl,
            kind = kind,
            scope = scope,
            relatedSpotIds = relatedSpotIds.toList(),
            localizations = localizations,
        )

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

            /** 英小文字・数字をハイフンで繋いだ形。[fromTitle] が生成する slug の形式。 */
            private val SLUG_PATTERN = Regex("^[a-z0-9]+(-[a-z0-9]+)*$")

            fun of(value: String): Id = Id(value.trim())

            /**
             * 英語タイトルから slug を生成する（`No littering` → `no-littering`）。
             *
             * 運営に ID を入力させないための入口。ID の命名は運営の関心事ではなく、手入力に
             * すると表記ゆれ・typo・重複が運用の負担になる（ジャンルの `GenreCode.fromLabel`
             * と同じ判断）。英数字以外は区切りとして扱う。日本語から作らないのは、ローマ字化の
             * 規則を持たないと意味のある slug にならないため。
             *
             * 生成結果が空になる場合（記号だけ等）は null を返し、呼び出し側に委ねる。
             * ここで既定値を返すと、意図しない ID が黙って残る。
             */
            fun fromTitle(title: String): Id? {
                val slug =
                    title
                        .trim()
                        .lowercase()
                        .replace(Regex("[^a-z0-9]+"), "-")
                        .trim('-')
                        .take(MAX_LENGTH)
                        .trim('-')

                return if (SLUG_PATTERN.matches(slug)) Id(slug) else null
            }
        }
    }

    companion object {
        fun create(
            id: Id,
            icon: MannerIcon? = null,
            iconUrl: MannerIconUrl? = null,
            kind: MannerKind,
            scope: MannerScope,
            localizations: MannerLocalizations,
            relatedSpotIds: List<RelatedSpotId> = emptyList(),
        ): MannerItem =
            MannerItem(
                id = id,
                icon = icon,
                iconUrl = iconUrl,
                kind = kind,
                scope = scope,
                relatedSpotIds = relatedSpotIds.toList(),
                localizations = localizations,
            )
    }
}

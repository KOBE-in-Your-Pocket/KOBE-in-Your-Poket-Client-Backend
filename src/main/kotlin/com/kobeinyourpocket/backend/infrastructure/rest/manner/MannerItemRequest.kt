package com.kobeinyourpocket.backend.infrastructure.rest.manner

import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerIcon
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerIconUrl
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerKind
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerLocalization
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerLocalizations
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerScope
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.RelatedSpotId
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty

/**
 * マナー項目の登録・更新リクエスト。
 *
 * **id は受け取らない。** 登録時は英語タイトルから生成し、更新時はパスの値を使う
 * （Client の項目詳細の遷移先になるため変更させない）。
 *
 * アイコンは [icon]（識別キー）と [iconUrl]（アップロード画像の URL）のどちらか一方が要る。
 * 両方 null の項目は Client がアイコンを描けないため、集約の不変条件で弾かれる。
 */
data class MannerItemRequest(
    /** アイコン識別キー。画像でアイコンを指定する場合は null でよい。 */
    val icon: String? = null,
    /** アップロード済み画像の URL。キーで指定する場合は null でよい。 */
    val iconUrl: String? = null,
    @field:NotBlank
    val kind: String = "",
    @field:NotBlank
    val scope: String = "",
    /** 関連する Spot.id。ID 参照のみで実在検証はしない（要件定義 M-2）。 */
    val relatedSpotIds: List<String> = emptyList(),
    /** 言語コード → 文言。対応言語すべてが必要。 */
    @field:NotEmpty
    val localizations: Map<String, LocalizationBody> = emptyMap(),
) {
    data class LocalizationBody(
        @field:NotBlank
        val title: String = "",
        @field:NotBlank
        val description: String = "",
    )

    fun toIcon(): MannerIcon? = icon?.takeIf { it.isNotBlank() }?.let { MannerIcon.of(it) }

    fun toIconUrl(): MannerIconUrl? = iconUrl?.takeIf { it.isNotBlank() }?.let { MannerIconUrl.of(it) }

    fun toKind(): MannerKind = MannerKind.of(kind)

    fun toScope(): MannerScope = MannerScope.of(scope)

    fun toRelatedSpotIds(): List<RelatedSpotId> = relatedSpotIds.distinct().map { RelatedSpotId.of(it) }

    /**
     * ドメインの VO に変換する。未対応の言語コードは捨てる。
     *
     * 「全言語そろっているか」の検証は application 層のポリシー（`requireAllLanguages`）に
     * 任せる。ここで先に弾くと、同じ規則が REST 層と application 層の 2 箇所に散る。
     */
    fun toLocalizations(): MannerLocalizations =
        MannerLocalizations.of(
            localizations
                .mapNotNull { (code, body) ->
                    Language.of(code)?.let { it to MannerLocalization(title = body.title, description = body.description) }
                }.toMap(),
        )
}

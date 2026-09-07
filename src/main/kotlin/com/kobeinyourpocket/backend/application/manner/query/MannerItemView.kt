package com.kobeinyourpocket.backend.application.manner.query

/**
 * 言語解決済みマナー項目の読みモデル（Client `domain/manner-item.ts` の `MannerItem` 形）。
 *
 * CQRS read 側専用。command 側の集約 [com.kobeinyourpocket.backend.domain.manner.manneritem.model.MannerItem] とは別経路。
 * kind / scope は Client リテラル（`manner|rule` / `local|japan`）のコード文字列で保持する。
 *
 * [title] / [description] は要求言語で解決済み（Client 用）。[localizations] は**全言語**を持つ。
 * 運営の管理画面は編集フォームで全言語を必要とし、言語ごとに取り直すと 1 項目あたり 4 往復に
 * なる（スポット詳細が実際そうなっている）。マナー項目は運営が管理するマスタで件数が限られる
 * ため、まとめて返しても負荷にならない（ジャンルマスタと同じ判断）。
 */
data class MannerItemView(
    val id: String,
    val title: String,
    val description: String,
    /** アイコン識別キー。画像 URL のみの項目では null。 */
    val icon: String?,
    /** アップロードされたアイコン画像の URL。未設定なら null。 */
    val iconUrl: String?,
    val kind: String,
    val scope: String,
    val relatedSpotIds: List<String>,
    /** 言語コード → 文言。収録されている言語ぶんが入る。 */
    val localizations: Map<String, MannerLocalizationView>,
)

/** 1 言語ぶんの文言。 */
data class MannerLocalizationView(
    val title: String,
    val description: String,
)

package com.kobeinyourpocket.backend.domain.manner.manneritem.vo

/**
 * [値オブジェクト] マナー項目の言語別ローカライズ内容。
 *
 * 単一言語ぶんの言語依存フィールド（title / description）をまとめる。
 * 言語非依存ベースは `model.MannerItem` 側に持ち、本オブジェクトは [MannerLocalizations] 経由で保持される。
 * DB `manner_item_localization(title, description)` の 1 行に対応する。
 */
data class MannerLocalization(
    val title: String,
    val description: String,
) {
    init {
        require(title.isNotBlank()) { "title must not be blank" }
        require(description.isNotBlank()) { "description must not be blank" }
    }
}

package com.kobeinyourpocket.backend.domain.manner.manneritem.vo

/**
 * マナー項目の言語別ローカライズ内容（値オブジェクト）。
 *
 * 単一言語ぶんの言語依存フィールド（title / description）をまとめる。
 * 内容はマナーの本質だが可変・多言語ゆえ同一性を持たないため VO とする。
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

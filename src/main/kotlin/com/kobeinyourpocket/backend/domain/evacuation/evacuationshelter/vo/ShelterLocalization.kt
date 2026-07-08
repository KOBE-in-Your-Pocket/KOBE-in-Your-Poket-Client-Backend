package com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo

/**
 * 避難所の言語別ローカライズ内容（値オブジェクト）。
 *
 * 単一言語ぶんの言語依存フィールド（name / address）をまとめる。
 * DB `evacuation_shelter_localization(name, address)` の 1 行に対応する。
 */
data class ShelterLocalization(
    val name: String,
    val address: String,
) {
    init {
        require(name.isNotBlank()) { "name must not be blank" }
        require(address.isNotBlank()) { "address must not be blank" }
    }
}

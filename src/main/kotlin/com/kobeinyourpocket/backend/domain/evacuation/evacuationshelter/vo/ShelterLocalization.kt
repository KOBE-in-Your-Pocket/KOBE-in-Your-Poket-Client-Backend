package com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo

/**
 * 避難所の言語別ローカライズ内容（値オブジェクト）。
 *
 * 単一言語ぶんの言語依存フィールド（name / address / note）をまとめる。
 * DB `shelter_localization(name, address, note)` の 1 行に対応する。
 *
 * [note] は元データの「備考」。「《土砂災害時》正門が土砂災害警戒区域内にあるので注意、
 * 早めに避難」のように避難の判断そのものに関わるため、言語別に持って翻訳して出す（#180）。
 * 備考が無い避難所が大半なので任意。
 */
data class ShelterLocalization(
    val name: String,
    val address: String,
    val note: String? = null,
) {
    init {
        require(name.isNotBlank()) { "name must not be blank" }
        require(address.isNotBlank()) { "address must not be blank" }
        require(note == null || note.isNotBlank()) { "note must not be blank when present" }
    }
}

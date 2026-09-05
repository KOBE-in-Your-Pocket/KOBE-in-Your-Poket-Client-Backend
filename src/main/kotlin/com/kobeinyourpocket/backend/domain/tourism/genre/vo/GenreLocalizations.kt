package com.kobeinyourpocket.backend.domain.tourism.genre.vo

import com.kobeinyourpocket.backend.domain.common.localization.Language

/**
 * [値オブジェクト] ジャンルの言語別表示名（#153）。
 *
 * **対応言語すべてを必須**にしている。1 言語でも欠けると、その言語のアプリでジャンル名が
 * 出せず、ラベルが無いという現状の問題がマスタ化しても残るため。スポットのローカライズが
 * en だけを不変条件にしているのと違い、ジャンルは件数が少なく入力コストも低い。
 */
data class GenreLocalizations(
    val byLanguage: Map<Language, String>,
) {
    init {
        val missing = Language.entries.filterNot { byLanguage[it]?.isNotBlank() == true }
        require(missing.isEmpty()) {
            "GenreLocalizations must contain a non-blank label for every language. missing=${missing.map { it.code }}"
        }
    }

    /** 要求 [language] の表示名。全言語必須なので必ず非 null。 */
    fun resolve(language: Language): String = byLanguage.getValue(language)

    companion object {
        /** 言語マップから防御的コピーを取り [GenreLocalizations] を生成する。 */
        fun of(byLanguage: Map<Language, String>): GenreLocalizations = GenreLocalizations(byLanguage.mapValues { it.value.trim() })
    }
}

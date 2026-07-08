package com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo

import com.kobeinyourpocket.backend.domain.tourism.localization.Language

/**
 * 避難所の言語別ローカライズ集合（値オブジェクト）。
 *
 * 不変条件はフォールバック言語 [FALLBACK]（en）の存在のみ。これにより [resolve] は必ず値を返せる（totality）。
 * 「全対応言語を提供」は登録時ポリシー（application 層）で扱い、**ドメインの不変条件にはしない**。
 */
data class ShelterLocalizations(
    val byLanguage: Map<Language, ShelterLocalization>,
) {
    init {
        require(byLanguage.containsKey(FALLBACK)) {
            "ShelterLocalizations must contain the fallback language '${FALLBACK.code}'"
        }
    }

    /** 収録されている言語の集合。 */
    val languages: Set<Language>
        get() = byLanguage.keys

    /**
     * 要求 [language] のローカライズを返す。無ければ [FALLBACK]（en）へフォールバックする。
     * en は `init` の不変条件で常に存在するため、本メソッドは必ず非 null を返す。
     */
    fun resolve(language: Language): ShelterLocalization = byLanguage[language] ?: byLanguage.getValue(FALLBACK)

    companion object {
        /** フォールバック言語（Client `FALLBACK_LANGUAGE` / Manner と同型）。 */
        val FALLBACK: Language = Language.EN

        /** 言語マップから防御的コピーを取り生成する。 */
        fun of(byLanguage: Map<Language, ShelterLocalization>): ShelterLocalizations = ShelterLocalizations(byLanguage.toMap())
    }
}

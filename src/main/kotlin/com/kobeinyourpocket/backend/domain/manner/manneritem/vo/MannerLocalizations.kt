package com.kobeinyourpocket.backend.domain.manner.manneritem.vo

import com.kobeinyourpocket.backend.domain.tourism.localization.Language

/**
 * マナー項目の言語別ローカライズ集合（値オブジェクト）。
 *
 * 不変条件はフォールバック言語 [FALLBACK]（en）の存在のみ。これにより [resolve] は必ず値を返せる（totality）。
 * 「全対応言語を提供」は登録時ポリシー（application 層）で扱い、**ドメインの不変条件にはしない**。
 * 対応言語は将来追加されうるため、全言語必須を集約に固定すると既存データが言語追加で壊れる。
 * `en` は Client の `FALLBACK_LANGUAGE` かつ削除されない基準なので、言語追加に対して安定した不変条件となる。
 */
data class MannerLocalizations(
    val byLanguage: Map<Language, MannerLocalization>,
) {
    init {
        require(byLanguage.containsKey(FALLBACK)) {
            "MannerLocalizations must contain the fallback language '${FALLBACK.code}'"
        }
    }

    /** 収録されている言語の集合。 */
    val languages: Set<Language>
        get() = byLanguage.keys

    /**
     * 要求 [language] のローカライズを返す。無ければ [FALLBACK]（en）へフォールバックする。
     * en は `init` の不変条件で常に存在するため、本メソッドは必ず非 null を返す。
     */
    fun resolve(language: Language): MannerLocalization = byLanguage[language] ?: byLanguage.getValue(FALLBACK)

    companion object {
        /** フォールバック言語（Client `FALLBACK_LANGUAGE` / 要件定義 D1）。 */
        val FALLBACK: Language = Language.EN

        /** 言語マップから防御的コピーを取り生成する。 */
        fun of(byLanguage: Map<Language, MannerLocalization>): MannerLocalizations = MannerLocalizations(byLanguage.toMap())
    }
}

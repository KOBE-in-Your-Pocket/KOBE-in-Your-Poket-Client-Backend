package com.kobeinyourpocket.backend.domain.manner.manneritem.vo

import com.kobeinyourpocket.backend.domain.tourism.localization.Language

/**
 * [値オブジェクト] マナー項目の言語別ローカライズ集合。
 *
 * 言語 → [MannerLocalization] の対応を保持し、要求言語が無ければ [FALLBACK]（en）へフォールバックして解決する。
 * フォールバック言語は要件定義 D1（既定フォールバックは en に統一）に従い、Tourism の ja 既定とは異なる。
 * フォールバック先である en の存在を不変条件として保証する。[Companion.of] が生成入口。
 */
data class MannerLocalizations(
    val byLanguage: Map<Language, MannerLocalization>,
) {
    init {
        require(byLanguage.containsKey(FALLBACK)) {
            "MannerLocalizations must contain the fallback language '${FALLBACK.code}' for fallback"
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
        /** 既定フォールバック言語（要件定義 D1: 全言語必須提供・既定は en に統一）。 */
        val FALLBACK: Language = Language.EN

        /** 言語マップから防御的コピーを取り [MannerLocalizations] を生成する。 */
        fun of(byLanguage: Map<Language, MannerLocalization>): MannerLocalizations = MannerLocalizations(byLanguage.toMap())
    }
}

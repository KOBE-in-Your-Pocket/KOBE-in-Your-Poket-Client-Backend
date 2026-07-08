package com.kobeinyourpocket.backend.domain.tourism.spot.vo

import com.kobeinyourpocket.backend.domain.tourism.localization.Language

/**
 * [値オブジェクト] スポットの言語別ローカライズ集合。
 *
 * 言語 → [SpotLocalization] の対応を保持し、要求言語が無ければ [Language.DEFAULT]（en）へ
 * フォールバックして解決する（モックの `resolveLocalization` 相当 / §7.1）。
 * フォールバック先である en の存在を不変条件として保証する。[Companion.of] が生成入口。
 */
data class SpotLocalizations(
    val byLanguage: Map<Language, SpotLocalization>,
) {
    init {
        require(byLanguage.containsKey(Language.DEFAULT)) {
            "SpotLocalizations must contain the default language '${Language.DEFAULT.code}' for fallback"
        }
    }

    /** 収録されている言語の集合。 */
    val languages: Set<Language>
        get() = byLanguage.keys

    /**
     * 要求 [language] のローカライズを返す。無ければ [Language.DEFAULT]（en）へフォールバックする。
     * en は `init` の不変条件で常に存在するため、本メソッドは必ず非 null を返す。
     */
    fun resolve(language: Language): SpotLocalization = byLanguage[language] ?: byLanguage.getValue(Language.DEFAULT)

    companion object {
        /** 言語マップから防御的コピーを取り [SpotLocalizations] を生成する。 */
        fun of(byLanguage: Map<Language, SpotLocalization>): SpotLocalizations = SpotLocalizations(byLanguage.toMap())
    }
}

package com.kobeinyourpocket.backend.infrastructure.rest.common

import com.kobeinyourpocket.backend.domain.common.localization.Language

/**
 * `?lang=` を主、`Accept-Language` ヘッダーを従として要求言語を解決する共通ロジック（§8 / #74）。
 *
 * tourism / evacuation / manner いずれの Controller からも共用する。
 * 対応言語コードがどちらからも解決できなければ [Language.DEFAULT] へフォールバックする。
 */
object LanguageResolver {
    fun resolve(
        lang: String?,
        acceptLanguage: String?,
    ): Language =
        lang?.let { Language.of(it) }
            ?: acceptLanguage?.let(::parseAcceptLanguage)
            ?: Language.DEFAULT

    private fun parseAcceptLanguage(header: String): Language? =
        header
            .split(',')
            .map { it.substringBefore(';').substringBefore('-').trim() }
            .firstNotNullOfOrNull { Language.of(it) }
}

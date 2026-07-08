package com.kobeinyourpocket.backend.infrastructure.rest.common

import com.kobeinyourpocket.backend.domain.common.localization.Language
import kotlin.test.Test
import kotlin.test.assertEquals

class LanguageResolverTest {
    @Test
    fun `lang を優先して解決する`() {
        assertEquals(Language.EN, LanguageResolver.resolve(lang = "en", acceptLanguage = "ja"))
    }

    @Test
    fun `lang が無ければ Accept-Language を解決する`() {
        assertEquals(Language.KO, LanguageResolver.resolve(lang = null, acceptLanguage = "ko-KR,ko;q=0.9,en;q=0.8"))
    }

    @Test
    fun `lang が非対応コードでも Accept-Language があればそちらへフォールバックする`() {
        assertEquals(Language.ZH, LanguageResolver.resolve(lang = "fr", acceptLanguage = "zh-CN"))
    }

    @Test
    fun `lang も Accept-Language も無ければ既定言語へフォールバックする`() {
        assertEquals(Language.DEFAULT, LanguageResolver.resolve(lang = null, acceptLanguage = null))
    }

    @Test
    fun `どちらも非対応コードなら既定言語へフォールバックする`() {
        assertEquals(Language.DEFAULT, LanguageResolver.resolve(lang = "fr", acceptLanguage = "de-DE"))
    }
}

package com.kobeinyourpocket.backend.infrastructure.web.tourism

import com.kobeinyourpocket.backend.application.tourism.SpotService
import com.kobeinyourpocket.backend.domain.tourism.vo.Language
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Spot の REST inbound adapter（§8）。application 経由のみ（persistence 直叩き禁止 / §2）。
 *
 * 言語は `?lang=` を主・`Accept-Language` を従とし、未指定・未対応なら ja へフォールバックする
 * （Client / README の「無指定は ja」と一致）。解決後の言語で application に問い合わせる。
 */
@RestController
@RequestMapping("/api/v1/tourism/spots")
class SpotController(
    private val spotService: SpotService,
) {
    @GetMapping
    fun listSpots(
        @RequestParam(name = "lang", required = false) lang: String?,
        @RequestHeader(name = "Accept-Language", required = false) acceptLanguage: String?,
    ): List<SpotResponse> {
        val language = resolveLanguage(lang, acceptLanguage)
        return spotService.listSpots(language).map(SpotResponse::from)
    }

    /**
     * `?lang=` 主 / `Accept-Language` 従で言語を解決する。どちらも解決不能なら [Language.DEFAULT]（ja）。
     */
    private fun resolveLanguage(
        lang: String?,
        acceptLanguage: String?,
    ): Language =
        lang?.let { Language.of(it) }
            ?: acceptLanguage?.let(::parseAcceptLanguage)
            ?: Language.DEFAULT

    /**
     * `Accept-Language`（例: `ko-KR,ko;q=0.9,en;q=0.8`）を優先度順に走査し、
     * 最初に対応する [Language] を返す。対応が無ければ null。
     */
    private fun parseAcceptLanguage(header: String): Language? =
        header
            .split(',')
            .map { it.substringBefore(';').substringBefore('-').trim() }
            .firstNotNullOfOrNull { Language.of(it) }
}

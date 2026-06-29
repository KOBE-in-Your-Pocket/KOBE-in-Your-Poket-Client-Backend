package com.kobeinyourpocket.backend.features.tourism.infrastructure.rest

import com.kobeinyourpocket.backend.features.tourism.application.query.ListSpotsService
import com.kobeinyourpocket.backend.features.tourism.domain.vo.Language
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Spot の REST inbound adapter（§8）。application 経由のみ（persistence 直叩き禁止 / §2）。
 */
@RestController
@RequestMapping("/api/v1/tourism/spots")
class SpotController(
    private val listSpotsService: ListSpotsService,
) {
    @GetMapping
    fun listSpots(
        @RequestParam(name = "lang", required = false) lang: String?,
        @RequestHeader(name = "Accept-Language", required = false) acceptLanguage: String?,
    ): List<SpotResponse> {
        val language = resolveLanguage(lang, acceptLanguage)
        return listSpotsService.listSpots(language).map(SpotResponse::from)
    }

    private fun resolveLanguage(
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

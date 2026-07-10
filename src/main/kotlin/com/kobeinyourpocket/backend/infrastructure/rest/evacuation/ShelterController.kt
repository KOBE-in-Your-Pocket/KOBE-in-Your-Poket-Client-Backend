package com.kobeinyourpocket.backend.infrastructure.rest.evacuation

import com.kobeinyourpocket.backend.application.evacuation.query.ListSheltersService
import com.kobeinyourpocket.backend.infrastructure.rest.common.LanguageResolver
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 避難所の REST inbound adapter（§8）。application 経由のみ（persistence 直叩き禁止 / §2）。
 *
 * 言語は `?lang=` 主・`Accept-Language` 従・en フォールバック（要件定義 D1）。
 */
@RestController
@RequestMapping("/api/v1/evacuation/shelters")
class ShelterController(
    private val listSheltersService: ListSheltersService,
) {
    @GetMapping
    fun listShelters(
        @RequestParam(name = "lang", required = false) lang: String?,
        @RequestHeader(name = "Accept-Language", required = false) acceptLanguage: String?,
    ): List<ShelterResponse> {
        val language = LanguageResolver.resolve(lang, acceptLanguage)
        return listSheltersService.listShelters(language).map(ShelterResponse::from)
    }
}

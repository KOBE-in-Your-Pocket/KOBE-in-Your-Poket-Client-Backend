package com.kobeinyourpocket.backend.infrastructure.rest.manner

import com.kobeinyourpocket.backend.application.manner.query.ListMannerItemsService
import com.kobeinyourpocket.backend.infrastructure.rest.common.LanguageResolver
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Manner の REST inbound adapter（§8 / M-1）。application 経由のみ（persistence 直叩き禁止 / §2）。
 *
 * kind / scope / spotId のクエリ絞り込みは実装しない（M-1: 一覧取得後の Client 側フィルタで成立するため）。
 * 言語は `?lang=` 主・`Accept-Language` 従・en フォールバック（D1）。
 */
@RestController
@RequestMapping("/api/v1/manner/items")
class MannerItemController(
    private val listMannerItemsService: ListMannerItemsService,
) {
    @GetMapping
    fun listMannerItems(
        @RequestParam(name = "lang", required = false) lang: String?,
        @RequestHeader(name = "Accept-Language", required = false) acceptLanguage: String?,
    ): List<MannerItemResponse> {
        val language = LanguageResolver.resolve(lang, acceptLanguage)
        return listMannerItemsService.listMannerItems(language).map(MannerItemResponse::from)
    }
}

package com.kobeinyourpocket.backend.infrastructure.rest.evacuation

import com.kobeinyourpocket.backend.application.evacuation.query.GetShelterListService
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
 * レスポンスは `data` + `meta` の封筒形（#85）。`meta` はデータセット全体の
 * 出典・データ基準日・最終更新日時（Client の起動時差分チェック用）。
 * data・meta は [GetShelterListService] が同一スナップショットでまとめて取得する。
 *
 * **閲覧のみで、書き込みの経路は持たない。** 避難所は神戸市が指定するもので、データの正は
 * 神戸市オープンデータにある（#180）。運営が個別に追加・削除できると出典と食い違い、
 * 「市が指定しているのにアプリに載っていない」状態を作ってしまう。更新はスナップショットの
 * 差し替え（`scripts/shelters/build_seed.py` で再生成し、マイグレーションを追加）で行う。
 */
@RestController
@RequestMapping("/api/v1/evacuation/shelters")
class ShelterController(
    private val getShelterListService: GetShelterListService,
) {
    @GetMapping
    fun listShelters(
        @RequestParam(name = "lang", required = false) lang: String?,
        @RequestHeader(name = "Accept-Language", required = false) acceptLanguage: String?,
    ): ShelterListResponse {
        val language = LanguageResolver.resolve(lang, acceptLanguage)
        val (shelters, metadata) = getShelterListService.getShelterList(language)
        return ShelterListResponse.of(shelters, metadata)
    }
}

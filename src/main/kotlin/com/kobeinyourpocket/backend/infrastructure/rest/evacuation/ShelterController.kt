package com.kobeinyourpocket.backend.infrastructure.rest.evacuation

import com.kobeinyourpocket.backend.application.evacuation.command.DeleteShelterService
import com.kobeinyourpocket.backend.application.evacuation.query.GetShelterListService
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.model.EvacuationShelter
import com.kobeinyourpocket.backend.infrastructure.rest.common.LanguageResolver
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
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
 */
@RestController
@RequestMapping("/api/v1/evacuation/shelters")
class ShelterController(
    private val getShelterListService: GetShelterListService,
    private val deleteShelterService: DeleteShelterService,
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

    /**
     * 指定避難所を削除する（運営ロール限定 / #144）。
     *
     * データ誤り・施設閉鎖時の運用手段。閲覧系は SecurityConfig で permitAll のため、
     * この API を守るのはメソッドセキュリティ側になる。ロール階層で ADMIN も通る。
     */
    @DeleteMapping("/{shelterId}")
    @PreAuthorize("hasRole('OPERATOR')")
    fun deleteShelter(
        @PathVariable shelterId: String,
    ): ResponseEntity<Void> {
        deleteShelterService.execute(EvacuationShelter.Id.of(shelterId))
        return ResponseEntity.noContent().build()
    }
}

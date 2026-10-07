package com.kobeinyourpocket.backend.infrastructure.rest.evacuation

import com.kobeinyourpocket.backend.application.evacuation.command.DeleteShelterService
import com.kobeinyourpocket.backend.application.evacuation.query.GetShelterListService
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.model.EvacuationShelter
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.DisasterType
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
    /**
     * 避難所一覧を返す。
     *
     * `?disaster=` で災害種別（`landslide` / `flood` / `tsunami` / `large-fire`）を指定すると、
     * **いずれか**の種別で ○（`suitable`）の避難所だけに絞る。△（条件付き）は含めない。
     * カンマ区切り（`?disaster=flood,tsunami`）と繰り返し（`?disaster=flood&disaster=tsunami`）の
     * どちらでも指定できる。省略時は全件（公開済み 1.0.0 の挙動のまま）。未知の種別は 400。
     *
     * 絞り込んでも `meta` はデータセット全体のもの。Client は絞り込み結果を
     * オフライン用の全件データとして保存しないこと。
     */
    @GetMapping
    fun listShelters(
        @RequestParam(name = "lang", required = false) lang: String?,
        @RequestHeader(name = "Accept-Language", required = false) acceptLanguage: String?,
        @RequestParam(name = "disaster", required = false) disaster: List<String>?,
    ): ShelterListResponse {
        val language = LanguageResolver.resolve(lang, acceptLanguage)
        val (shelters, metadata) = getShelterListService.getShelterList(language, parseDisasterTypes(disaster))
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

    /** 空要素（`?disaster=` や末尾カンマ）は無視する。未知の種別は 400 にして、黙って全件を返さない。 */
    private fun parseDisasterTypes(values: List<String>?): Set<DisasterType> =
        values
            .orEmpty()
            .filter(String::isNotBlank)
            .map { DisasterType.of(it) ?: throw IllegalArgumentException("Unknown disaster type: $it") }
            .toSet()
}

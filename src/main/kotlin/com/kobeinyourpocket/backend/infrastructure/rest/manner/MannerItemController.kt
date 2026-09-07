package com.kobeinyourpocket.backend.infrastructure.rest.manner

import com.kobeinyourpocket.backend.application.manner.command.DeleteMannerItemService
import com.kobeinyourpocket.backend.application.manner.command.RegisterMannerItemService
import com.kobeinyourpocket.backend.application.manner.command.UpdateMannerItemService
import com.kobeinyourpocket.backend.application.manner.query.ListMannerItemsService
import com.kobeinyourpocket.backend.domain.manner.manneritem.model.MannerItem
import com.kobeinyourpocket.backend.infrastructure.rest.common.LanguageResolver
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * Manner の REST inbound adapter（§8 / M-1）。application 経由のみ（persistence 直叩き禁止 / §2）。
 *
 * kind / scope / spotId のクエリ絞り込みは実装しない（M-1: 一覧取得後の Client 側フィルタで成立するため）。
 * 言語は `?lang=` 主・`Accept-Language` 従・en フォールバック（D1）。
 *
 * 一覧は Client が使うため公開。書き込みは運営ロール限定（ロール階層で ADMIN も通る）。
 */
@RestController
@RequestMapping("/api/v1/manner/items")
class MannerItemController(
    private val listMannerItemsService: ListMannerItemsService,
    private val registerMannerItemService: RegisterMannerItemService,
    private val updateMannerItemService: UpdateMannerItemService,
    private val deleteMannerItemService: DeleteMannerItemService,
) {
    /** 解決済みの文言に加え、全言語の localizations も返す（[MannerItemResponse] 参照）。 */
    @GetMapping
    fun listMannerItems(
        @RequestParam(name = "lang", required = false) lang: String?,
        @RequestHeader(name = "Accept-Language", required = false) acceptLanguage: String?,
    ): List<MannerItemResponse> {
        val language = LanguageResolver.resolve(lang, acceptLanguage)
        return listMannerItemsService.listMannerItems(language).map(MannerItemResponse::from)
    }

    /** id は英語タイトルから自動生成する。運営には入力させない。 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('OPERATOR')")
    fun registerMannerItem(
        @Valid @RequestBody request: MannerItemRequest,
    ): MannerItemResponse =
        MannerItemResponse.from(
            registerMannerItemService.registerMannerItem(
                icon = request.toIcon(),
                iconUrl = request.toIconUrl(),
                kind = request.toKind(),
                scope = request.toScope(),
                relatedSpotIds = request.toRelatedSpotIds(),
                localizations = request.toLocalizations(),
            ),
        )

    /** 全置換。id はパスの値のまま（Client の項目詳細の遷移先のため変更させない）。 */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OPERATOR')")
    fun updateMannerItem(
        @PathVariable id: String,
        @Valid @RequestBody request: MannerItemRequest,
    ): MannerItemResponse =
        MannerItemResponse.from(
            updateMannerItemService.updateMannerItem(
                id = MannerItem.Id.of(id),
                icon = request.toIcon(),
                iconUrl = request.toIconUrl(),
                kind = request.toKind(),
                scope = request.toScope(),
                relatedSpotIds = request.toRelatedSpotIds(),
                localizations = request.toLocalizations(),
            ),
        )

    /** 関連スポットとローカライズは DB の ON DELETE CASCADE で連動削除される。 */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OPERATOR')")
    fun deleteMannerItem(
        @PathVariable id: String,
    ): ResponseEntity<Void> {
        deleteMannerItemService.deleteMannerItem(MannerItem.Id.of(id))
        return ResponseEntity.noContent().build()
    }
}

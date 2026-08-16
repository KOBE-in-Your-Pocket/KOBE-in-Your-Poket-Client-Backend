package com.kobeinyourpocket.backend.infrastructure.rest.tourism

import com.kobeinyourpocket.backend.application.tourism.command.DeleteReviewService
import com.kobeinyourpocket.backend.application.tourism.query.ListAllReviewsService
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewId
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
 * 運営向けレビュー管理の REST inbound adapter（#165）。
 *
 * スポット配下の [ReviewController]（`/spots/{spotId}/reviews`）が Client 向けの公開 API なのに対し、
 * こちらは**管理画面用の横断ビュー**。全スポットを 1 リクエストで並べ、モデレーション削除を行う。
 * パスを分けているのは、横断一覧が `spotId` を取らないため（クラス単位の mapping を共有できない）。
 *
 * `reviewId` は UUID の主キーで全体一意のため、削除に `spotId` は要求しない。
 *
 * 認可は一覧・削除とも運営ロール限定。閲覧系は SecurityConfig で permitAll のため、
 * 一覧を守るのはメソッドセキュリティ側になる。公開の一覧（スポット別）と違い、
 * **全スポットの投稿者情報を 1 リクエストで取得できてしまう**ため公開しない。
 * ロール階層で ADMIN も通る。
 */
@RestController
@RequestMapping("/api/v1/tourism/reviews")
class ReviewModerationController(
    private val listAllReviewsService: ListAllReviewsService,
    private val deleteReviewService: DeleteReviewService,
) {
    /**
     * レビューを全スポット横断で新しい順に返す。
     *
     * `?lang=` は**スポット名の解決にのみ**効く。レビュー本文・投稿者名は投稿時の言語のまま返る。
     */
    @GetMapping
    @PreAuthorize("hasRole('OPERATOR')")
    fun listReviews(
        @RequestParam(name = "page", required = false) page: Int?,
        @RequestParam(name = "size", required = false) size: Int?,
        @RequestParam(name = "lang", required = false) lang: String?,
        @RequestHeader(name = "Accept-Language", required = false) acceptLanguage: String?,
    ): ReviewListResponse {
        val language = LanguageResolver.resolve(lang, acceptLanguage)
        return ReviewListResponse.from(listAllReviewsService.listReviews(language, page, size))
    }

    /** 不適切なレビューを削除する（要件表 C-9）。投稿者本人かどうかは問わない。 */
    @DeleteMapping("/{reviewId}")
    @PreAuthorize("hasRole('OPERATOR')")
    fun deleteReview(
        @PathVariable reviewId: String,
    ): ResponseEntity<Void> {
        deleteReviewService.execute(ReviewId.of(reviewId))
        return ResponseEntity.noContent().build()
    }
}

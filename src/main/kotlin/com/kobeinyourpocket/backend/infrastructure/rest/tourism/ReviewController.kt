package com.kobeinyourpocket.backend.infrastructure.rest.tourism

import com.kobeinyourpocket.backend.application.tourism.command.DeleteOwnReviewService
import com.kobeinyourpocket.backend.application.tourism.command.PostReviewService
import com.kobeinyourpocket.backend.application.tourism.command.UpdateReviewService
import com.kobeinyourpocket.backend.application.tourism.query.ListReviewsService
import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewAuthorId
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewId
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewRating
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId
import com.kobeinyourpocket.backend.infrastructure.rest.common.LanguageResolver
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
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
 * Review の REST inbound adapter（§8）。application 経由のみ（persistence 直叩き禁止 / §2）。
 *
 * 書き込み（POST / PUT / DELETE）は SecurityConfig で認証必須。投稿者は JWT の `sub` から
 * 取り、リクエストボディの自己申告は使わない（#86）。本人かどうかの判定は application 側。
 */
@RestController
@RequestMapping("/api/v1/tourism/spots/{spotId}/reviews")
class ReviewController(
    private val postReviewService: PostReviewService,
    private val listReviewsService: ListReviewsService,
    private val updateReviewService: UpdateReviewService,
    private val deleteOwnReviewService: DeleteOwnReviewService,
) {
    @GetMapping
    fun listReviews(
        @PathVariable spotId: String,
        @RequestParam(name = "lang", required = false) lang: String?,
        @RequestHeader(name = "Accept-Language", required = false) acceptLanguage: String?,
    ): List<ReviewResponse> {
        val language = LanguageResolver.resolve(lang, acceptLanguage)
        return listReviewsService
            .listReviews(SpotId.of(spotId), language)
            .map(ReviewResponse::from)
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun postReview(
        @PathVariable spotId: String,
        @Valid @RequestBody request: ReviewRequest,
        @AuthenticationPrincipal jwt: Jwt,
    ): ReviewResponse {
        val review =
            postReviewService.postReview(
                spotId = SpotId.of(spotId),
                rating = ReviewRating.of(request.rating),
                comment = request.comment,
                authorName = request.author.name,
                authorUserId = requesterId(jwt),
                language = Language.of(request.language) ?: Language.DEFAULT,
            )
        return ReviewResponse.from(review)
    }

    @PutMapping("/{reviewId}")
    fun updateReview(
        @PathVariable spotId: String,
        @PathVariable reviewId: String,
        @Valid @RequestBody request: ReviewUpdateRequest,
        @AuthenticationPrincipal jwt: Jwt,
    ): ReviewResponse {
        val updated =
            updateReviewService.updateReview(
                reviewId = ReviewId.of(reviewId),
                rating = ReviewRating.of(request.rating),
                comment = request.comment,
                requesterId = requesterId(jwt),
            )
        return ReviewResponse.from(updated)
    }

    /**
     * 自分が投稿したレビューを削除する（#86）。
     *
     * 他人の投稿は 403、存在しない ID は 404。運営による削除は
     * `DELETE /api/v1/tourism/reviews/{reviewId}`（[ReviewModerationController]）で別経路。
     */
    @DeleteMapping("/{reviewId}")
    fun deleteOwnReview(
        @PathVariable spotId: String,
        @PathVariable reviewId: String,
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<Void> {
        deleteOwnReviewService.execute(ReviewId.of(reviewId), requesterId(jwt))
        return ResponseEntity.noContent().build()
    }

    /**
     * JWT から投稿者 id を取り出す。
     *
     * SecurityConfig が書き込みを `authenticated()` にしているため、ここに来た時点で
     * JWT は検証済み。`sub` 欠落は Supabase 側の異常なので握り潰さず落とす。
     */
    private fun requesterId(jwt: Jwt): ReviewAuthorId {
        val subject = requireNotNull(jwt.subject) { "JWT subject is missing" }
        return ReviewAuthorId.of(subject)
    }
}

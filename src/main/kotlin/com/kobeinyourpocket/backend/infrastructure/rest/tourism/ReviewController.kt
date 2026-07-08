package com.kobeinyourpocket.backend.infrastructure.rest.tourism

import com.kobeinyourpocket.backend.application.tourism.command.PostReviewService
import com.kobeinyourpocket.backend.application.tourism.command.UpdateReviewService
import com.kobeinyourpocket.backend.application.tourism.query.ListReviewsService
import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewId
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewRating
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId
import com.kobeinyourpocket.backend.infrastructure.rest.common.LanguageResolver
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
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
 */
@RestController
@RequestMapping("/api/v1/tourism/spots/{spotId}/reviews")
class ReviewController(
    private val postReviewService: PostReviewService,
    private val listReviewsService: ListReviewsService,
    private val updateReviewService: UpdateReviewService,
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
    ): ReviewResponse {
        val review =
            postReviewService.postReview(
                spotId = SpotId.of(spotId),
                rating = ReviewRating.of(request.rating),
                comment = request.comment,
                authorName = request.author.name,
                language = Language.of(request.language) ?: Language.DEFAULT,
            )
        return ReviewResponse.from(review)
    }

    @PutMapping("/{reviewId}")
    fun updateReview(
        @PathVariable spotId: String,
        @PathVariable reviewId: String,
        @Valid @RequestBody request: ReviewUpdateRequest,
    ): ReviewResponse {
        val updated =
            updateReviewService.updateReview(
                reviewId = ReviewId.of(reviewId),
                rating = ReviewRating.of(request.rating),
                comment = request.comment,
            )
        return ReviewResponse.from(updated)
    }
}

package com.kobeinyourpocket.backend.infrastructure.rest.report

import com.kobeinyourpocket.backend.application.report.command.ReportReviewService
import com.kobeinyourpocket.backend.domain.report.vo.ReportReason
import com.kobeinyourpocket.backend.domain.report.vo.ReporterId
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewId
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * レビュー通報の REST inbound adapter（#147）。
 *
 * パスはレビュー API（[com.kobeinyourpocket.backend.infrastructure.rest.tourism.ReviewController]）の
 * 配下だが、通報は report コンテキストの責務なのでこちらに置く。
 *
 * SecurityConfig で認証必須（一般ロール可）。通報者は JWT の `sub` から取る。
 */
@RestController
class ReviewReportController(
    private val reportReviewService: ReportReviewService,
) {
    /**
     * レビューを通報する。
     *
     * - 201: 受け付けた
     * - 400: 理由の code が不正 / `OTHER` で詳細が空 / 詳細が長すぎる / 自分のレビュー
     * - 404: レビューが存在しない、またはパスのスポット配下にない
     * - 409: 同じレビューをすでに通報している
     */
    @PostMapping("/api/v1/tourism/spots/{spotId}/reviews/{reviewId}/reports")
    @ResponseStatus(HttpStatus.CREATED)
    fun reportReview(
        @PathVariable spotId: String,
        @PathVariable reviewId: String,
        @Valid @RequestBody request: ReviewReportRequest,
        @AuthenticationPrincipal jwt: Jwt,
    ): ReportResponse {
        val reason =
            ReportReason.of(request.reason)
                ?: throw IllegalArgumentException("Unknown report reason: ${request.reason}")
        val report =
            reportReviewService.execute(
                spotId = SpotId.of(spotId),
                reviewId = ReviewId.of(reviewId),
                reporterId = ReporterId.of(requireNotNull(jwt.subject) { "JWT subject is missing" }),
                reason = reason,
                description = request.description,
            )
        return ReportResponse.from(report)
    }
}

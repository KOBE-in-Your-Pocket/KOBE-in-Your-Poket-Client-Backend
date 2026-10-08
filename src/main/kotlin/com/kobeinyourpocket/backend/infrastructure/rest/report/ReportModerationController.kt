package com.kobeinyourpocket.backend.infrastructure.rest.report

import com.kobeinyourpocket.backend.application.report.command.HandleReviewReportsService
import com.kobeinyourpocket.backend.application.report.query.ListReviewReportsService
import com.kobeinyourpocket.backend.domain.report.vo.ReportHandlerId
import com.kobeinyourpocket.backend.domain.report.vo.ReportStatus
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewId
import com.kobeinyourpocket.backend.infrastructure.rest.common.LanguageResolver
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 運営向け通報管理の REST inbound adapter（#145）。
 *
 * 一覧・更新とも運営ロール限定（ロール階層で ADMIN も通る）。閲覧系は SecurityConfig で
 * permitAll のため、一覧を守るのはメソッドセキュリティ側になる。通報者の id と名前を含むため公開しない。
 */
@RestController
@RequestMapping("/api/v1/reports/reviews")
class ReportModerationController(
    private val listReviewReportsService: ListReviewReportsService,
    private val handleReviewReportsService: HandleReviewReportsService,
) {
    /**
     * 通報された口コミを、未対応の通報が多い順に返す。
     *
     * `?status=OPEN` で未対応の通報がある口コミだけに絞る（`RESOLVED` / `DISMISSED` も可）。
     * `?lang=` はスポット名の解決にだけ効く。
     */
    @GetMapping
    @PreAuthorize("hasRole('OPERATOR')")
    fun listReviewReports(
        @RequestParam(name = "status", required = false) status: String?,
        @RequestParam(name = "page", required = false) page: Int?,
        @RequestParam(name = "size", required = false) size: Int?,
        @RequestParam(name = "lang", required = false) lang: String?,
        @RequestHeader(name = "Accept-Language", required = false) acceptLanguage: String?,
    ): ReviewReportListResponse {
        val language = LanguageResolver.resolve(lang, acceptLanguage)
        val view =
            listReviewReportsService.listReviewReports(
                language = language,
                status = status?.takeIf(String::isNotBlank)?.let(::parseStatus),
                page = page,
                size = size,
            )
        return ReviewReportListResponse.from(view)
    }

    /**
     * 口コミへの未対応の通報をまとめて対応済み（`RESOLVED`）か却下（`DISMISSED`）にする。
     *
     * - 200: 更新した（全件対応済みなら `updatedCount: 0`）
     * - 400: `status` が不正、または `OPEN`
     * - 404: その口コミへの通報が 1 件も無い
     */
    @PatchMapping("/{reviewId}")
    @PreAuthorize("hasRole('OPERATOR')")
    fun handleReviewReports(
        @PathVariable reviewId: String,
        @Valid @RequestBody request: HandleReviewReportsRequest,
        @AuthenticationPrincipal jwt: Jwt,
    ): HandleReviewReportsResponse {
        val status = parseStatus(request.status)
        val updated =
            handleReviewReportsService.execute(
                reviewId = ReviewId.of(reviewId),
                status = status,
                handledBy = ReportHandlerId.of(requireNotNull(jwt.subject) { "JWT subject is missing" }),
            )
        return HandleReviewReportsResponse(reviewId = reviewId, status = status.name, updatedCount = updated)
    }

    private fun parseStatus(value: String): ReportStatus =
        ReportStatus.of(value) ?: throw IllegalArgumentException("Unknown report status: $value")
}

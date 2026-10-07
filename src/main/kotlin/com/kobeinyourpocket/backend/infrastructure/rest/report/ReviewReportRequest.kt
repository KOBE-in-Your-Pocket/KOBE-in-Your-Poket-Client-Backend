package com.kobeinyourpocket.backend.infrastructure.rest.report

import com.kobeinyourpocket.backend.domain.report.model.Report
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * `POST /api/v1/tourism/spots/{spotId}/reviews/{reviewId}/reports` のリクエストボディ（#147）。
 *
 * [reason] は `ReportReason` の code（`SPAM` / `HARASSMENT` / `HATE` / `SEXUAL_OR_VIOLENT` /
 * `PERSONAL_INFO` / `MISLEADING` / `OTHER`）。`OTHER` のときだけ [description] が必須で、
 * その判定はドメイン（`Report`）が行う。
 */
data class ReviewReportRequest(
    @field:NotBlank
    val reason: String,
    @field:Size(max = Report.MAX_DESCRIPTION_LENGTH)
    val description: String? = null,
)

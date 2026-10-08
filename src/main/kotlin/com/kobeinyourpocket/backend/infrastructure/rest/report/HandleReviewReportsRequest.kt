package com.kobeinyourpocket.backend.infrastructure.rest.report

import jakarta.validation.constraints.NotBlank

/**
 * `PATCH /api/v1/reports/reviews/{reviewId}` のリクエストボディ（#145）。
 *
 * [status] は `APPROVED`（承認・非表示に同意）か `DISMISSED`（却下・非表示にしない）。`OPEN` へ戻す操作は無い。
 */
data class HandleReviewReportsRequest(
    @field:NotBlank
    val status: String,
)

/** 対応状況を変えた結果。[updatedCount] は今回閉じた通報の件数（全件対応済みなら 0）。 */
data class HandleReviewReportsResponse(
    val reviewId: String,
    val status: String,
    val updatedCount: Int,
)

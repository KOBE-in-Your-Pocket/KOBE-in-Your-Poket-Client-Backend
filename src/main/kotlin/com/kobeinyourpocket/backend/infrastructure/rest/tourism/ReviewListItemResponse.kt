package com.kobeinyourpocket.backend.infrastructure.rest.tourism

import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.kobeinyourpocket.backend.application.tourism.query.ReviewView

/**
 * `GET /api/v1/tourism/spots/{spotId}/reviews` の 1 件。[ReviewResponse] に [hiddenByReport] を足した形。
 *
 * POST / PUT の応答（[ReviewResponse]）には載せない。書き込み側の集約は通報の状態を持たず、
 * 正しい値を返せないため。
 */
data class ReviewListItemResponse(
    @field:JsonUnwrapped
    val review: ReviewResponse,
    /** 通報が運営に承認された口コミか。true なら Client は非表示にする。 */
    val hiddenByReport: Boolean,
) {
    companion object {
        fun from(view: ReviewView): ReviewListItemResponse =
            ReviewListItemResponse(
                review = ReviewResponse.from(view),
                hiddenByReport = view.hiddenByReport,
            )
    }
}

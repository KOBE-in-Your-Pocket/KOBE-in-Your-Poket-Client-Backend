package com.kobeinyourpocket.backend.application.tourism.query

import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId
import org.springframework.stereotype.Service

/** レビュー一覧取得ユースケース（read）。domain 集約を経由せず [ReviewQuery] port へ委譲する。 */
@Service
class ListReviewsService(
    private val reviewQuery: ReviewQuery,
) {
    fun listReviews(
        spotId: SpotId,
        language: Language,
    ): List<ReviewView> = reviewQuery.findBySpot(spotId, language)
}

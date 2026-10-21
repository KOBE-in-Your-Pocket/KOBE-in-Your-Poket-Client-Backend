package com.kobeinyourpocket.backend.application.tourism.query

import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewAuthorId
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId
import org.springframework.stereotype.Service

/**
 * レビュー一覧取得ユースケース（read）。domain 集約を経由せず [ReviewQuery] port へ委譲する。
 *
 * [language] が null なら全言語のレビューを返す。
 * ログイン中の閲覧なら [viewerId] を渡す。閲覧者がブロック中の投稿者のレビューは返さない（#215）。
 */
@Service
class ListReviewsService(
    private val reviewQuery: ReviewQuery,
) {
    fun listReviews(
        spotId: SpotId,
        language: Language?,
        viewerId: ReviewAuthorId? = null,
    ): List<ReviewView> = reviewQuery.findBySpot(spotId, language, viewerId)
}

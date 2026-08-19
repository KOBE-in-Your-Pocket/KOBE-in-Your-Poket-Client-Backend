package com.kobeinyourpocket.backend.application.tourism

import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewId

/** 指定 [ReviewId] のレビューが存在しない場合の例外（REST では 404 / #165）。 */
class ReviewNotFoundException(
    id: ReviewId,
) : RuntimeException("Review not found: ${id.value}")

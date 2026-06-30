package com.kobeinyourpocket.backend.domain.tourism.repository

import com.kobeinyourpocket.backend.domain.tourism.aggregate.Review
import com.kobeinyourpocket.backend.domain.tourism.vo.ReviewId

/** write port（command）。read は application.tourism.query.ReviewQuery。 */
interface ReviewRepository {
    fun save(review: Review): Review

    fun findById(id: ReviewId): Review?
}

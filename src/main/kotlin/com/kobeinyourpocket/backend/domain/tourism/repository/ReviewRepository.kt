package com.kobeinyourpocket.backend.domain.tourism.repository

import com.kobeinyourpocket.backend.domain.tourism.aggregate.Review

/** write 専用 port（command）。read は application.tourism.query.ReviewQuery。 */
interface ReviewRepository {
    fun save(review: Review): Review
}

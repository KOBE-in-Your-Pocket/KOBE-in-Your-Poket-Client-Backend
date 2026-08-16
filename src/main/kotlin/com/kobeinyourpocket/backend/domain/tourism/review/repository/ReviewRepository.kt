package com.kobeinyourpocket.backend.domain.tourism.review.repository

import com.kobeinyourpocket.backend.domain.tourism.review.model.Review
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewId

/** [リポジトリ] write port（command）。read は application.tourism.query.ReviewQuery。 */
interface ReviewRepository {
    fun save(review: Review): Review

    fun findById(id: ReviewId): Review?

    fun existsById(id: ReviewId): Boolean

    /** レビューを削除する。集約に子は無いため単独で消える（#165）。 */
    fun deleteById(id: ReviewId)
}

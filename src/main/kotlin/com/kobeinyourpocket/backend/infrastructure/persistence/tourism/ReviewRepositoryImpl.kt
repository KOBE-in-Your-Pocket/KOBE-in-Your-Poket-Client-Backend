package com.kobeinyourpocket.backend.infrastructure.persistence.tourism

import com.kobeinyourpocket.backend.domain.tourism.aggregate.Review
import com.kobeinyourpocket.backend.domain.tourism.repository.ReviewRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

/** [ReviewRepository] port の outbound adapter（write のみ）。 */
@Repository
class ReviewRepositoryImpl(
    private val reviewJpa: ReviewJpaRepository,
) : ReviewRepository {
    @Transactional
    override fun save(review: Review): Review {
        reviewJpa.save(ReviewEntity.fromDomain(review))
        return review
    }
}

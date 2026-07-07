package com.kobeinyourpocket.backend.application.tourism.command

import com.kobeinyourpocket.backend.domain.tourism.review.model.Review
import com.kobeinyourpocket.backend.domain.tourism.review.repository.ReviewRepository
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewId
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewRating
import org.springframework.stereotype.Service

/** レビュー更新ユースケース（write）。rating と comment のみ更新可能。 */
@Service
class UpdateReviewService(
    private val reviewRepository: ReviewRepository,
) {
    fun updateReview(
        reviewId: ReviewId,
        rating: ReviewRating,
        comment: String,
    ): Review {
        val existing =
            reviewRepository.findById(reviewId) ?: error("Review not found: $reviewId")
        return reviewRepository.save(existing.copy(rating = rating, comment = comment))
    }
}

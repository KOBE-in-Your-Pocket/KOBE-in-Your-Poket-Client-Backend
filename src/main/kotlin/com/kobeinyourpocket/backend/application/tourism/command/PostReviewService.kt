package com.kobeinyourpocket.backend.application.tourism.command

import com.kobeinyourpocket.backend.domain.tourism.aggregate.Review
import com.kobeinyourpocket.backend.domain.tourism.repository.ReviewRepository
import com.kobeinyourpocket.backend.domain.tourism.vo.Language
import com.kobeinyourpocket.backend.domain.tourism.vo.ReviewAuthor
import com.kobeinyourpocket.backend.domain.tourism.vo.ReviewRating
import com.kobeinyourpocket.backend.domain.tourism.vo.SpotId
import org.springframework.stereotype.Service

/** レビュー投稿ユースケース（write）。投稿者は author seam として受ける（認証確定後に差し替え）。 */
@Service
class PostReviewService(
    private val reviewRepository: ReviewRepository,
) {
    fun postReview(
        spotId: SpotId,
        rating: ReviewRating,
        comment: String,
        author: ReviewAuthor,
        language: Language,
    ): Review {
        val review =
            Review.create(
                spotId = spotId,
                rating = rating,
                comment = comment,
                author = author,
                language = language,
            )
        return reviewRepository.save(review)
    }
}

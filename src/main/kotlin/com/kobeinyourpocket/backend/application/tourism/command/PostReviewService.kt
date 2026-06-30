package com.kobeinyourpocket.backend.application.tourism.command

import com.kobeinyourpocket.backend.domain.tourism.aggregate.Review
import com.kobeinyourpocket.backend.domain.tourism.repository.ReviewRepository
import com.kobeinyourpocket.backend.domain.tourism.vo.Language
import com.kobeinyourpocket.backend.domain.tourism.vo.ReviewAuthor
import com.kobeinyourpocket.backend.domain.tourism.vo.ReviewRating
import com.kobeinyourpocket.backend.domain.tourism.vo.SpotId
import org.springframework.stereotype.Service

/** レビュー投稿ユースケース（write）。投稿者は authorName を引数で受ける薄い seam。 */
@Service
class PostReviewService(
    private val reviewRepository: ReviewRepository,
) {
    fun postReview(
        spotId: SpotId,
        rating: ReviewRating,
        comment: String,
        authorName: String,
        language: Language,
    ): Review {
        val review =
            Review.create(
                spotId = spotId,
                rating = rating,
                comment = comment,
                author = ReviewAuthor(name = authorName),
                language = language,
            )
        return reviewRepository.save(review)
    }
}

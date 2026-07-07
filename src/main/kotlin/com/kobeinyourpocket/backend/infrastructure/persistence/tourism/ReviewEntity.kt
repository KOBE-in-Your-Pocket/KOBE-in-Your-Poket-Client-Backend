package com.kobeinyourpocket.backend.infrastructure.persistence.tourism

import com.kobeinyourpocket.backend.domain.tourism.localization.Language
import com.kobeinyourpocket.backend.domain.tourism.review.model.Review
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewAuthor
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewId
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewRating
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/** DB `review`。createdAt は domain 側で採番した値をそのまま保存する。 */
@Entity
@Table(name = "review")
class ReviewEntity(
    @Id
    @Column(name = "id", columnDefinition = "uuid", updatable = false, nullable = false)
    var id: UUID,
    @Column(name = "spot_id", nullable = false)
    var spotId: String,
    @Column(name = "rating", nullable = false)
    var rating: Int,
    @Column(name = "comment", nullable = false)
    var comment: String,
    @Column(name = "author_name", nullable = false)
    var authorName: String,
    @Column(name = "author_icon_url", nullable = false)
    var authorIconUrl: String = "",
    @Column(name = "language", nullable = false)
    var language: String,
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant,
) {
    fun toDomain(): Review {
        val lang = Language.of(language) ?: error("Unknown language code in review: '$language'")
        return Review(
            id = ReviewId.of(id),
            spotId = SpotId.of(spotId),
            rating = ReviewRating.of(rating),
            comment = comment,
            author = ReviewAuthor(name = authorName, iconUrl = authorIconUrl.ifEmpty { null }),
            createdAt = createdAt,
            language = lang,
        )
    }

    companion object {
        fun fromDomain(review: Review): ReviewEntity =
            ReviewEntity(
                id = review.id.value,
                spotId = review.spotId.value,
                rating = review.rating.value,
                comment = review.comment,
                authorName = review.author.name,
                authorIconUrl = review.author.iconUrl.orEmpty(),
                language = review.language.code,
                createdAt = review.createdAt,
            )
    }
}

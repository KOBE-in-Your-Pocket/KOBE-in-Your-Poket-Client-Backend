package com.kobeinyourpocket.backend.infrastructure.query.tourism

import com.kobeinyourpocket.backend.application.tourism.query.ReviewQuery
import com.kobeinyourpocket.backend.application.tourism.query.ReviewView
import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.Instant
import java.time.OffsetDateTime

/** [ReviewQuery] の JPA 実装。スポット別・言語別でレビューを取得する。 */
@Repository
class ReviewQueryJpa(
    private val entityManager: EntityManager,
) : ReviewQuery {
    override fun findBySpot(
        spotId: SpotId,
        language: Language,
    ): List<ReviewView> {
        @Suppress("UNCHECKED_CAST")
        val rows =
            entityManager
                .createNativeQuery(
                    """
                    SELECT id, spot_id, rating, comment, author_name, author_icon_url, created_at, language
                    FROM review
                    WHERE spot_id = :spotId AND language = :language
                    ORDER BY created_at DESC
                    """.trimIndent(),
                ).apply {
                    setParameter("spotId", spotId.value)
                    setParameter("language", language.code)
                }.resultList as List<Array<Any?>>

        return rows.map(::toReviewView)
    }

    private fun toInstant(value: Any?): Instant =
        when (value) {
            is OffsetDateTime -> value.toInstant()
            is Timestamp -> value.toInstant()
            else -> (value as java.util.Date).toInstant()
        }

    private fun toReviewView(row: Array<Any?>): ReviewView =
        ReviewView(
            id = row[0].toString(),
            spotId = row[1] as String,
            rating = (row[2] as Number).toInt(),
            comment = row[3] as String,
            authorName = row[4] as String,
            authorIconUrl = (row[5] as String).ifEmpty { null },
            createdAt = toInstant(row[6]),
            language = row[7] as String,
        )
}

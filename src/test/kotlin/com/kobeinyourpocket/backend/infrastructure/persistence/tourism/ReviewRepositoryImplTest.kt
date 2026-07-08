package com.kobeinyourpocket.backend.infrastructure.persistence.tourism

import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.domain.tourism.review.model.Review
import com.kobeinyourpocket.backend.domain.tourism.review.repository.ReviewRepository
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewAuthor
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewRating
import com.kobeinyourpocket.backend.domain.tourism.spot.model.Spot
import com.kobeinyourpocket.backend.domain.tourism.spot.model.SpotWithLocalizations
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.Coordinates
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.Genre
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotLocalization
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotLocalizations
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotMedia
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(SpotRepositoryImpl::class, ReviewRepositoryImpl::class)
class ReviewRepositoryImplTest {
    @Autowired
    private lateinit var spotRepository: SpotRepositoryImpl

    @Autowired
    private lateinit var repository: ReviewRepository

    @Autowired
    private lateinit var reviewJpa: ReviewJpaRepository

    private val spotId = SpotId.of("kobe-port-tower")
    private val now = Instant.parse("2025-11-03T10:24:00Z")

    /** review は spot への FK を持つため、事前に spot を挿入する。 */
    private fun insertSpot() {
        spotRepository.save(
            SpotWithLocalizations(
                spot =
                    Spot(
                        id = spotId,
                        genre = Genre.LANDMARK,
                        coordinates = Coordinates.of(34.6826, 135.1863),
                        media = SpotMedia("https://example.com/kobe-port-tower.webp"),
                    ),
                localizations =
                    SpotLocalizations.of(
                        mapOf(
                            Language.JA to
                                SpotLocalization("神戸ポートタワー", "ランドマーク", "説明", "9:00-23:00", "神戸市中央区波止場町5-5"),
                            Language.EN to
                                SpotLocalization("Kobe Port Tower", "Landmark", "Desc", "9:00-23:00", "5-5 Hatobacho, Chuo-ku, Kobe"),
                        ),
                    ),
            ),
        )
    }

    @Test
    fun `save で review が永続化される`() {
        insertSpot()
        val review =
            Review.create(
                spotId = spotId,
                rating = ReviewRating.of(5),
                comment = "Great spot!",
                author = ReviewAuthor(name = "Alice", iconUrl = "https://example.com/alice.png"),
                language = Language.EN,
                createdAt = now,
            )

        repository.save(review)

        val entity = reviewJpa.findById(review.id.value).orElseThrow()
        assertEquals(5, entity.rating)
        assertEquals("Great spot!", entity.comment)
        assertEquals("Alice", entity.authorName)
        assertEquals("https://example.com/alice.png", entity.authorIconUrl)
        assertEquals("en", entity.language)
        assertEquals(now, entity.createdAt)
    }

    @Test
    fun `iconUrl なしの review も永続化できる`() {
        insertSpot()
        val review =
            Review.create(
                spotId = spotId,
                rating = ReviewRating.of(3),
                comment = "良い場所です",
                author = ReviewAuthor(name = "Bob"),
                language = Language.JA,
                createdAt = now,
            )

        repository.save(review)

        val entity = reviewJpa.findById(review.id.value).orElseThrow()
        assertEquals("", entity.authorIconUrl)
    }
}

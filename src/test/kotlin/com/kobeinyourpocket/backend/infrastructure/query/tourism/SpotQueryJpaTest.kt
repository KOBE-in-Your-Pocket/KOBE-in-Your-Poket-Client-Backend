package com.kobeinyourpocket.backend.infrastructure.query.tourism

import com.kobeinyourpocket.backend.domain.tourism.aggregate.Review
import com.kobeinyourpocket.backend.domain.tourism.aggregate.Spot
import com.kobeinyourpocket.backend.domain.tourism.aggregate.SpotWithLocalizations
import com.kobeinyourpocket.backend.domain.tourism.repository.ReviewRepository
import com.kobeinyourpocket.backend.domain.tourism.repository.SpotRepository
import com.kobeinyourpocket.backend.domain.tourism.vo.Coordinates
import com.kobeinyourpocket.backend.domain.tourism.vo.Genre
import com.kobeinyourpocket.backend.domain.tourism.vo.Language
import com.kobeinyourpocket.backend.domain.tourism.vo.ReviewAuthor
import com.kobeinyourpocket.backend.domain.tourism.vo.ReviewRating
import com.kobeinyourpocket.backend.domain.tourism.vo.SpotId
import com.kobeinyourpocket.backend.domain.tourism.vo.SpotLocalization
import com.kobeinyourpocket.backend.domain.tourism.vo.SpotLocalizations
import com.kobeinyourpocket.backend.domain.tourism.vo.SpotMedia
import com.kobeinyourpocket.backend.infrastructure.persistence.tourism.ReviewRepositoryImpl
import com.kobeinyourpocket.backend.infrastructure.persistence.tourism.SpotRepositoryImpl
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(SpotRepositoryImpl::class, ReviewRepositoryImpl::class, SpotQueryJpa::class)
class SpotQueryJpaTest {
    @Autowired
    private lateinit var spotRepository: SpotRepository

    @Autowired
    private lateinit var reviewRepository: ReviewRepository

    @Autowired
    private lateinit var spotQuery: SpotQueryJpa

    private val portTower =
        SpotWithLocalizations(
            spot =
                Spot(
                    id = SpotId.of("kobe-port-tower"),
                    genre = Genre.LANDMARK,
                    coordinates = Coordinates.of(34.6826, 135.1863),
                    media = SpotMedia("https://example.com/kobe-port-tower.webp"),
                ),
            localizations =
                SpotLocalizations.of(
                    mapOf(
                        Language.JA to SpotLocalization("神戸ポートタワー", "ランドマーク", "神戸のシンボル。", "9:00-23:00"),
                        Language.EN to SpotLocalization("Kobe Port Tower", "Landmark", "The symbol of Kobe.", "9:00-23:00"),
                    ),
                ),
        )

    @Test
    fun `要求言語で解決した SpotView を返す`() {
        spotRepository.save(portTower)

        val result = spotQuery.findAllResolved(Language.EN).single()

        assertEquals("Kobe Port Tower", result.name)
        assertEquals("landmark", result.genre)
    }

    @Test
    fun `要求言語が無ければ ja へフォールバックする`() {
        spotRepository.save(portTower)

        val result = spotQuery.findAllResolved(Language.KO).single()

        assertEquals("神戸ポートタワー", result.name)
    }

    @Test
    fun `レビューが無ければ rating は null`() {
        spotRepository.save(portTower)

        val result = spotQuery.findAllResolved(Language.JA).single()

        assertNull(result.rating)
    }

    @Test
    fun `レビューが複数あれば rating はその平均`() {
        spotRepository.save(portTower)
        val spotId = SpotId.of("kobe-port-tower")
        val now = Instant.parse("2025-11-03T10:00:00Z")
        reviewRepository.save(
            Review.create(
                spotId = spotId,
                rating = ReviewRating.of(4),
                comment = "Good",
                author = ReviewAuthor(name = "Alice"),
                language = Language.EN,
                createdAt = now,
            ),
        )
        reviewRepository.save(
            Review.create(
                spotId = spotId,
                rating = ReviewRating.of(2),
                comment = "Okay",
                author = ReviewAuthor(name = "Bob"),
                language = Language.JA,
                createdAt = now,
            ),
        )

        val result = spotQuery.findAllResolved(Language.JA).single()

        assertEquals(3.0, result.rating)
    }
}

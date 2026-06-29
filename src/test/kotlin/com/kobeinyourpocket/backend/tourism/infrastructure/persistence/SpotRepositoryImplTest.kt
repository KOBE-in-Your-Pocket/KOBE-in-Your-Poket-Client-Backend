package com.kobeinyourpocket.backend.tourism.infrastructure.persistence

import com.kobeinyourpocket.backend.tourism.domain.aggregate.Spot
import com.kobeinyourpocket.backend.tourism.domain.aggregate.SpotWithLocalizations
import com.kobeinyourpocket.backend.tourism.domain.repository.SpotRepository
import com.kobeinyourpocket.backend.tourism.domain.vo.Coordinates
import com.kobeinyourpocket.backend.tourism.domain.vo.Genre
import com.kobeinyourpocket.backend.tourism.domain.vo.Language
import com.kobeinyourpocket.backend.tourism.domain.vo.SpotId
import com.kobeinyourpocket.backend.tourism.domain.vo.SpotLocalization
import com.kobeinyourpocket.backend.tourism.domain.vo.SpotLocalizations
import com.kobeinyourpocket.backend.tourism.domain.vo.SpotMedia
import com.kobeinyourpocket.backend.tourism.domain.vo.SpotRating
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(SpotRepositoryImpl::class)
class SpotRepositoryImplTest {
    @Autowired
    private lateinit var repository: SpotRepository

    @Autowired
    private lateinit var spotJpa: SpotJpaRepository

    private val portTower =
        SpotWithLocalizations(
            spot =
                Spot(
                    id = SpotId.of("kobe-port-tower"),
                    genre = Genre.LANDMARK,
                    coordinates = Coordinates.of(34.6826, 135.1863),
                    media = SpotMedia("https://example.com/kobe-port-tower.webp"),
                    rating = SpotRating(4.5),
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
    fun `save で spot と localization が永続化される`() {
        repository.save(portTower)

        val entity = spotJpa.findById("kobe-port-tower").orElseThrow()
        assertEquals("landmark", entity.genre)
        assertEquals(4.5, entity.ratingValue)
    }

    @Test
    fun `rating なしも永続化できる`() {
        repository.save(portTower.copy(spot = portTower.spot.copy(id = SpotId.of("no-rating"), rating = null)))

        val entity = spotJpa.findById("no-rating").orElseThrow()
        assertNull(entity.ratingValue)
    }
}

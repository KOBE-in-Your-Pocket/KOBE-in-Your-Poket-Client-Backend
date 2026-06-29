package com.kobeinyourpocket.backend.infrastructure.persistence.tourism

import com.kobeinyourpocket.backend.domain.tourism.aggregate.Spot
import com.kobeinyourpocket.backend.domain.tourism.aggregate.SpotWithLocalizations
import com.kobeinyourpocket.backend.domain.tourism.repository.SpotRepository
import com.kobeinyourpocket.backend.domain.tourism.vo.Coordinates
import com.kobeinyourpocket.backend.domain.tourism.vo.Genre
import com.kobeinyourpocket.backend.domain.tourism.vo.Language
import com.kobeinyourpocket.backend.domain.tourism.vo.SpotId
import com.kobeinyourpocket.backend.domain.tourism.vo.SpotLocalization
import com.kobeinyourpocket.backend.domain.tourism.vo.SpotLocalizations
import com.kobeinyourpocket.backend.domain.tourism.vo.SpotMedia
import com.kobeinyourpocket.backend.domain.tourism.vo.SpotRating
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
    fun `save した集約を findAll で全フィールド復元できる`() {
        repository.save(portTower)

        val restored = repository.findAll().single()

        assertEquals(SpotId.of("kobe-port-tower"), restored.spot.id)
        assertEquals(Genre.LANDMARK, restored.spot.genre)
        assertEquals(Coordinates.of(34.6826, 135.1863), restored.spot.coordinates)
        assertEquals("https://example.com/kobe-port-tower.webp", restored.spot.media.imageUrl)
        assertEquals(4.5, restored.spot.rating?.value)
        assertEquals("Kobe Port Tower", restored.localizations.resolve(Language.EN).name)
        assertEquals("神戸ポートタワー", restored.localizations.resolve(Language.KO).name)
    }

    @Test
    fun `rating なしも永続化できる`() {
        repository.save(portTower.copy(spot = portTower.spot.copy(id = SpotId.of("no-rating"), rating = null)))

        val restored = repository.findAll().single { it.spot.id == SpotId.of("no-rating") }

        assertNull(restored.spot.rating)
    }
}

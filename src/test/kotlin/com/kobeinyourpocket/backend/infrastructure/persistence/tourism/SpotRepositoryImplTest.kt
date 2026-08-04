package com.kobeinyourpocket.backend.infrastructure.persistence.tourism

import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.domain.tourism.spot.model.Spot
import com.kobeinyourpocket.backend.domain.tourism.spot.model.SpotWithLocalizations
import com.kobeinyourpocket.backend.domain.tourism.spot.repository.SpotRepository
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.Coordinates
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.Genre
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotLocalization
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotLocalizations
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotMedia
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotRating
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
                        Language.JA to
                            SpotLocalization(
                                "神戸ポートタワー",
                                "ランドマーク",
                                "神戸のシンボル。",
                                "9:00-23:00",
                                "神戸市中央区波止場町5-5",
                            ),
                        Language.EN to
                            SpotLocalization(
                                "Kobe Port Tower",
                                "Landmark",
                                "The symbol of Kobe.",
                                "9:00-23:00",
                                "5-5 Hatobacho, Chuo-ku, Kobe",
                            ),
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

    @Test
    fun `findSpotByIdForUpdate は行ロック付きで取得できる`() {
        // SELECT ... FOR UPDATE のクエリが実際に発行できることの確認（@DataJpaTest はトランザクション内）。
        repository.save(portTower)

        val found = repository.findSpotByIdForUpdate(SpotId.of("kobe-port-tower"))

        assertEquals(SpotId.of("kobe-port-tower"), found?.id)
        assertEquals(Genre.LANDMARK, found?.genre)
        assertEquals("https://example.com/kobe-port-tower.webp", found?.media?.imageUrl)
    }

    @Test
    fun `findSpotByIdForUpdate は該当なしで null`() {
        assertNull(repository.findSpotByIdForUpdate(SpotId.of("missing")))
    }
}

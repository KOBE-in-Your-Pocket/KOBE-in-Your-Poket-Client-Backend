package com.kobeinyourpocket.backend.application.tourism

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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SpotServiceTest {
    private class FakeSpotRepository : SpotRepository {
        val store = linkedMapOf<SpotId, SpotWithLocalizations>()

        override fun findAll(): List<SpotWithLocalizations> = store.values.toList()

        override fun save(spot: SpotWithLocalizations): SpotWithLocalizations {
            store[spot.spot.id] = spot
            return spot
        }
    }

    private val localizations =
        SpotLocalizations.of(
            mapOf(
                Language.JA to SpotLocalization("神戸ポートタワー", "ランドマーク", "神戸のシンボル。", "9:00-23:00"),
                Language.EN to SpotLocalization("Kobe Port Tower", "Landmark", "The symbol of Kobe.", "9:00-23:00"),
            ),
        )

    private fun service(repository: SpotRepository = FakeSpotRepository()) = SpotService(repository)

    @Test
    fun `listSpots は要求言語へ解決する`() {
        val repository = FakeSpotRepository()
        repository.save(
            SpotWithLocalizations(
                spot =
                    Spot(
                        id = SpotId.of("kobe-port-tower"),
                        genre = Genre.LANDMARK,
                        coordinates = Coordinates.of(34.6826, 135.1863),
                        media = SpotMedia("https://example.com/x.webp"),
                    ),
                localizations = localizations,
            ),
        )

        val result = service(repository).listSpots(Language.EN).single()

        assertEquals("Kobe Port Tower", result.localization.name)
    }

    @Test
    fun `listSpots は無い言語を ja へフォールバックする`() {
        val repository = FakeSpotRepository()
        repository.save(
            SpotWithLocalizations(
                spot =
                    Spot(
                        id = SpotId.of("kobe-port-tower"),
                        genre = Genre.LANDMARK,
                        coordinates = Coordinates.of(34.6826, 135.1863),
                        media = SpotMedia("https://example.com/x.webp"),
                    ),
                localizations = localizations,
            ),
        )

        val result = service(repository).listSpots(Language.KO).single()

        assertEquals("神戸ポートタワー", result.localization.name)
    }

    @Test
    fun `registerSpot は採番して保存し rating は null`() {
        val repository = FakeSpotRepository()

        val created =
            service(repository).registerSpot(
                genre = Genre.LANDMARK,
                coordinates = Coordinates.of(34.6826, 135.1863),
                media = SpotMedia("https://example.com/x.webp"),
                localizations = localizations,
            )

        val createdId = created.spot.id.value
        assertTrue(createdId.isNotBlank())
        assertNull(created.spot.rating)
        assertEquals(created, repository.store[created.spot.id])
        assertEquals("Kobe Port Tower", created.localizations.resolve(Language.EN).name)
    }
}

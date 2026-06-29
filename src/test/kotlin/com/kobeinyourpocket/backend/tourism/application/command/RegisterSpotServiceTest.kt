package com.kobeinyourpocket.backend.tourism.application.command

import com.kobeinyourpocket.backend.tourism.domain.aggregate.SpotWithLocalizations
import com.kobeinyourpocket.backend.tourism.domain.repository.SpotRepository
import com.kobeinyourpocket.backend.tourism.domain.vo.Coordinates
import com.kobeinyourpocket.backend.tourism.domain.vo.Genre
import com.kobeinyourpocket.backend.tourism.domain.vo.Language
import com.kobeinyourpocket.backend.tourism.domain.vo.SpotId
import com.kobeinyourpocket.backend.tourism.domain.vo.SpotLocalization
import com.kobeinyourpocket.backend.tourism.domain.vo.SpotLocalizations
import com.kobeinyourpocket.backend.tourism.domain.vo.SpotMedia
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RegisterSpotServiceTest {
    private class FakeSpotRepository : SpotRepository {
        val store = linkedMapOf<SpotId, SpotWithLocalizations>()

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

    @Test
    fun `registerSpot は採番して保存し rating は null`() {
        val repository = FakeSpotRepository()

        val created =
            RegisterSpotService(repository).registerSpot(
                genre = Genre.LANDMARK,
                coordinates = Coordinates.of(34.6826, 135.1863),
                media = SpotMedia("https://example.com/x.webp"),
                localizations = localizations,
            )

        assertTrue(
            created.spot.id.value
                .isNotBlank(),
        )
        assertNull(created.spot.rating)
        assertEquals(created, repository.store[created.spot.id])
    }
}

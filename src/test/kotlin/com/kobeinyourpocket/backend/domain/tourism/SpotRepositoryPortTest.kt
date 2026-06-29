package com.kobeinyourpocket.backend.domain.tourism.repository

import com.kobeinyourpocket.backend.domain.tourism.aggregate.Spot
import com.kobeinyourpocket.backend.domain.tourism.aggregate.SpotWithLocalizations
import com.kobeinyourpocket.backend.domain.tourism.vo.Coordinates
import com.kobeinyourpocket.backend.domain.tourism.vo.Genre
import com.kobeinyourpocket.backend.domain.tourism.vo.Language
import com.kobeinyourpocket.backend.domain.tourism.vo.SpotId
import com.kobeinyourpocket.backend.domain.tourism.vo.SpotLocalization
import com.kobeinyourpocket.backend.domain.tourism.vo.SpotLocalizations
import com.kobeinyourpocket.backend.domain.tourism.vo.SpotMedia
import kotlin.test.Test
import kotlin.test.assertEquals

/** SpotRepository write port の契約を Fake で検証する。 */
class SpotRepositoryPortTest {
    private class FakeSpotRepository : SpotRepository {
        private val store = linkedMapOf<SpotId, SpotWithLocalizations>()

        override fun save(spot: SpotWithLocalizations): SpotWithLocalizations {
            store[spot.spot.id] = spot
            return spot
        }

        fun get(id: SpotId): SpotWithLocalizations? = store[id]
    }

    @Test
    fun `save した集約を取得できる`() {
        val repository = FakeSpotRepository()
        val aggregate =
            SpotWithLocalizations(
                spot =
                    Spot.create(
                        id = SpotId.of("kobe-port-tower"),
                        genre = Genre.LANDMARK,
                        coordinates = Coordinates.of(34.6826, 135.1863),
                        media = SpotMedia("https://example.com/x.webp"),
                    ),
                localizations =
                    SpotLocalizations.of(
                        mapOf(
                            Language.JA to SpotLocalization("神戸ポートタワー", "ランドマーク", "説明", "9:00"),
                            Language.EN to SpotLocalization("Kobe Port Tower", "Landmark", "Desc", "9:00"),
                        ),
                    ),
            )

        repository.save(aggregate)

        assertEquals(aggregate, repository.get(SpotId.of("kobe-port-tower")))
    }
}

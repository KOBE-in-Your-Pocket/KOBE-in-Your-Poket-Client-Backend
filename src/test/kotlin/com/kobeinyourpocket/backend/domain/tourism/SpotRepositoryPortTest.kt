package com.kobeinyourpocket.backend.domain.tourism

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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** SpotRepository write port の契約を Fake で検証する。 */
class SpotRepositoryPortTest {
    private class FakeSpotRepository : SpotRepository {
        private val store = linkedMapOf<SpotId, SpotWithLocalizations>()

        override fun save(spot: SpotWithLocalizations): SpotWithLocalizations {
            store[spot.spot.id] = spot
            return spot
        }

        override fun existsById(id: SpotId): Boolean = store.containsKey(id)

        override fun deleteById(id: SpotId) {
            store.remove(id)
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
                            Language.JA to
                                SpotLocalization("神戸ポートタワー", "ランドマーク", "説明", "9:00", "神戸市中央区波止場町5-5"),
                            Language.EN to
                                SpotLocalization(
                                    "Kobe Port Tower",
                                    "Landmark",
                                    "Desc",
                                    "9:00",
                                    "5-5 Hatobacho, Chuo-ku, Kobe",
                                ),
                        ),
                    ),
            )

        repository.save(aggregate)

        assertEquals(aggregate, repository.get(SpotId.of("kobe-port-tower")))
    }

    @Test
    fun `deleteById で削除した後は existsById が false になる`() {
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
                            Language.JA to
                                SpotLocalization("神戸ポートタワー", "ランドマーク", "説明", "9:00", "神戸市中央区波止場町5-5"),
                            Language.EN to
                                SpotLocalization(
                                    "Kobe Port Tower",
                                    "Landmark",
                                    "Desc",
                                    "9:00",
                                    "5-5 Hatobacho, Chuo-ku, Kobe",
                                ),
                        ),
                    ),
            )
        repository.save(aggregate)
        assertTrue(repository.existsById(SpotId.of("kobe-port-tower")))

        repository.deleteById(SpotId.of("kobe-port-tower"))

        assertFalse(repository.existsById(SpotId.of("kobe-port-tower")))
        assertEquals(null, repository.get(SpotId.of("kobe-port-tower")))
    }
}

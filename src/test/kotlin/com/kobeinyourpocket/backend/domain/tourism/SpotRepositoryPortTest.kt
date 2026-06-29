package com.kobeinyourpocket.backend.domain.tourism

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

/** SpotRepository port をインメモリ Fake で実装し、契約と言語解決の合成を検証する。 */
class SpotRepositoryPortTest {
    private class FakeSpotRepository : SpotRepository {
        private val store = linkedMapOf<SpotId, SpotWithLocalizations>()

        override fun findAll(): List<SpotWithLocalizations> = store.values.toList()

        override fun save(spot: SpotWithLocalizations): SpotWithLocalizations {
            store[spot.spot.id] = spot
            return spot
        }
    }

    private fun spotWithLocalizations(
        id: String,
        localizations: Map<Language, SpotLocalization>,
    ): SpotWithLocalizations =
        SpotWithLocalizations(
            spot =
                Spot.create(
                    id = SpotId.of(id),
                    genre = Genre.LANDMARK,
                    coordinates = Coordinates.of(34.6826, 135.1863),
                    media = SpotMedia("https://example.com/$id.webp"),
                ),
            localizations = SpotLocalizations.of(localizations),
        )

    private val portTower =
        spotWithLocalizations(
            id = "kobe-port-tower",
            localizations =
                mapOf(
                    Language.JA to SpotLocalization("神戸ポートタワー", "ランドマーク", "神戸のシンボル。", "9:00-23:00"),
                    Language.EN to SpotLocalization("Kobe Port Tower", "Landmark", "The symbol of Kobe.", "9:00-23:00"),
                ),
        )

    @Test
    fun `save した集約を findAll で取得できる`() {
        val repository = FakeSpotRepository()

        repository.save(portTower)

        assertEquals(listOf(portTower), repository.findAll())
    }

    @Test
    fun `findAll の結果を要求言語へ解決でき、無ければ ja へフォールバックする`() {
        val repository = FakeSpotRepository()
        repository.save(portTower)

        val stored = repository.findAll().single()

        assertEquals("Kobe Port Tower", stored.localizations.resolve(Language.EN).name)
        assertEquals("神戸ポートタワー", stored.localizations.resolve(Language.KO).name)
    }
}

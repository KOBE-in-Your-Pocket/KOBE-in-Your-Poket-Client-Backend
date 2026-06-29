package com.kobeinyourpocket.backend.tourism.infrastructure.query

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
import com.kobeinyourpocket.backend.tourism.infrastructure.persistence.SpotRepositoryImpl
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import kotlin.test.Test
import kotlin.test.assertEquals

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(SpotRepositoryImpl::class, SpotQueryJpa::class)
class SpotQueryJpaTest {
    @Autowired
    private lateinit var spotRepository: SpotRepository

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
}

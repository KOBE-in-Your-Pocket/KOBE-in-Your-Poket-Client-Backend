package com.kobeinyourpocket.backend.application.tourism.command

import com.kobeinyourpocket.backend.application.tourism.query.SpotNotFoundException
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
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class UpdateSpotServiceTest {
    private val spotId = SpotId.of("kobe-port-tower")

    private val newLocalizations =
        SpotLocalizations.of(
            mapOf(
                Language.JA to
                    SpotLocalization(
                        "神戸ポートタワー（改）",
                        "展望",
                        "リニューアル後の説明。",
                        "10:00-22:00",
                        "神戸市中央区波止場町5-5",
                    ),
                Language.EN to
                    SpotLocalization(
                        "Kobe Port Tower (renewed)",
                        "Observation",
                        "Description after renewal.",
                        "10:00-22:00",
                        "5-5 Hatobacho, Chuo-ku, Kobe",
                    ),
            ),
        )

    @Test
    fun `updateSpot は id と rating を保持しつつ他フィールドを差し替えて保存する`() {
        val repository = mockk<SpotRepository>()
        val existing =
            Spot(
                id = spotId,
                genre = Genre.LANDMARK,
                coordinates = Coordinates.of(34.6826, 135.1863),
                media = SpotMedia("https://example.com/old.webp"),
                rating = SpotRating(4.2),
            )
        every { repository.findSpotById(spotId) } returns existing
        val saved = slot<SpotWithLocalizations>()
        every { repository.save(capture(saved)) } answers { saved.captured }

        val updated =
            UpdateSpotService(repository).updateSpot(
                id = spotId,
                genre = Genre.GOURMET,
                coordinates = Coordinates.of(34.7000, 135.2000),
                media = SpotMedia("https://example.com/new.webp"),
                localizations = newLocalizations,
            )

        // id と rating は不変
        assertEquals(spotId, updated.spot.id)
        assertEquals(SpotRating(4.2), updated.spot.rating)
        // 差し替え対象は反映
        assertEquals(Genre.GOURMET, updated.spot.genre)
        assertEquals(Coordinates.of(34.7000, 135.2000), updated.spot.coordinates)
        assertEquals("https://example.com/new.webp", updated.spot.media.imageUrl)
        assertSame(newLocalizations, updated.localizations)
        verify(exactly = 1) { repository.save(saved.captured) }
    }

    @Test
    fun `updateSpot は該当 id が無ければ SpotNotFoundException を投げ save しない`() {
        val repository = mockk<SpotRepository>()
        every { repository.findSpotById(spotId) } returns null

        assertFailsWith<SpotNotFoundException> {
            UpdateSpotService(repository).updateSpot(
                id = spotId,
                genre = Genre.GOURMET,
                coordinates = Coordinates.of(34.7000, 135.2000),
                media = SpotMedia("https://example.com/new.webp"),
                localizations = newLocalizations,
            )
        }

        verify(exactly = 0) { repository.save(any()) }
    }
}

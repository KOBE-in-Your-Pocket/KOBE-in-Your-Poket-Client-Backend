package com.kobeinyourpocket.backend.application.tourism.command

import com.kobeinyourpocket.backend.domain.tourism.localization.Language
import com.kobeinyourpocket.backend.domain.tourism.spot.model.SpotWithLocalizations
import com.kobeinyourpocket.backend.domain.tourism.spot.repository.SpotRepository
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.Coordinates
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.Genre
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotLocalization
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotLocalizations
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotMedia
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RegisterSpotServiceTest {
    private val localizations =
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
        )

    @Test
    fun `registerSpot は採番して保存し rating は null`() {
        val repository = mockk<SpotRepository>()
        val saved = slot<SpotWithLocalizations>()
        every { repository.save(capture(saved)) } answers { saved.captured }

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
        assertEquals(localizations, created.localizations)
        verify(exactly = 1) { repository.save(saved.captured) }
    }
}

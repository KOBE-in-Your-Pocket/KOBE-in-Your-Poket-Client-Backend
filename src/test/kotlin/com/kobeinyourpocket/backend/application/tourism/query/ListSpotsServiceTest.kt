package com.kobeinyourpocket.backend.application.tourism.query

import com.kobeinyourpocket.backend.domain.tourism.vo.Language
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals

class ListSpotsServiceTest {
    private val jaView =
        SpotView(
            id = "kobe-port-tower",
            name = "神戸ポートタワー",
            genre = "landmark",
            description = "神戸のシンボル。",
            latitude = 34.6826,
            longitude = 135.1863,
            businessHours = "9:00-23:00",
            categoryLabel = "ランドマーク",
            imageUrl = "https://example.com/x.webp",
            rating = 4.5,
        )

    private val enView = jaView.copy(name = "Kobe Port Tower", description = "The symbol of Kobe.", categoryLabel = "Landmark")

    @Test
    fun `要求言語を SpotQuery port に渡して解決済み SpotView を返す`() {
        val spotQuery = mockk<SpotQuery>()
        every { spotQuery.findAllResolved(Language.EN) } returns listOf(enView)

        val result = ListSpotsService(spotQuery).listSpots(Language.EN)

        assertEquals("Kobe Port Tower", result.single().name)
        verify(exactly = 1) { spotQuery.findAllResolved(Language.EN) }
    }

    @Test
    fun `要求言語のローカライズが無い場合は ja フォールバック済み SpotView を返す`() {
        val spotQuery = mockk<SpotQuery>()
        every { spotQuery.findAllResolved(Language.KO) } returns listOf(jaView)

        val result = ListSpotsService(spotQuery).listSpots(Language.KO)

        assertEquals("神戸ポートタワー", result.single().name)
        verify(exactly = 1) { spotQuery.findAllResolved(Language.KO) }
    }
}

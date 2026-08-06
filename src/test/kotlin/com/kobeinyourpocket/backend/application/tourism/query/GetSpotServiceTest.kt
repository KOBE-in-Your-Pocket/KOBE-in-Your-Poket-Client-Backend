package com.kobeinyourpocket.backend.application.tourism.query

import com.kobeinyourpocket.backend.application.tourism.SpotNotFoundException
import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GetSpotServiceTest {
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
            address = "神戸市中央区波止場町5-5",
        )

    @Test
    fun `該当 id があれば SpotQuery port が解決した SpotView を返す`() {
        val spotQuery = mockk<SpotQuery>()
        every { spotQuery.findByIdResolved(SpotId.of("kobe-port-tower"), Language.JA) } returns jaView

        val result = GetSpotService(spotQuery).getSpot(SpotId.of("kobe-port-tower"), Language.JA)

        assertEquals("神戸ポートタワー", result.name)
        verify(exactly = 1) { spotQuery.findByIdResolved(SpotId.of("kobe-port-tower"), Language.JA) }
    }

    @Test
    fun `該当 id が無ければ SpotNotFoundException を投げる`() {
        val spotQuery = mockk<SpotQuery>()
        every { spotQuery.findByIdResolved(SpotId.of("unknown"), Language.JA) } returns null

        assertFailsWith<SpotNotFoundException> {
            GetSpotService(spotQuery).getSpot(SpotId.of("unknown"), Language.JA)
        }
    }
}

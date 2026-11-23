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

    private val spotQuery = mockk<SpotQuery>()
    private val spotViewCountQuery = mockk<SpotViewCountQuery>()
    private val service = GetSpotService(spotQuery, spotViewCountQuery)

    @Test
    fun `該当 id があれば SpotQuery port が解決した SpotView と閲覧数を返す`() {
        every { spotQuery.findByIdResolved(SpotId.of("kobe-port-tower"), Language.JA) } returns jaView
        every { spotViewCountQuery.countBySpotId(SpotId.of("kobe-port-tower")) } returns 7

        val result = service.getSpot(SpotId.of("kobe-port-tower"), Language.JA)

        assertEquals(SpotDetailView(spot = jaView, viewCount = 7), result)
        verify(exactly = 1) { spotQuery.findByIdResolved(SpotId.of("kobe-port-tower"), Language.JA) }
    }

    @Test
    fun `該当 id が無ければ SpotNotFoundException を投げ、閲覧数は数えない`() {
        every { spotQuery.findByIdResolved(SpotId.of("unknown"), Language.JA) } returns null

        assertFailsWith<SpotNotFoundException> {
            service.getSpot(SpotId.of("unknown"), Language.JA)
        }
        verify(exactly = 0) { spotViewCountQuery.countBySpotId(any()) }
    }
}

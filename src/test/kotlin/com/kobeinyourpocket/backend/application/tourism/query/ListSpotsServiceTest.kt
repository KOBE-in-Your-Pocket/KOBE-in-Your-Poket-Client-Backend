package com.kobeinyourpocket.backend.application.tourism.query

import com.kobeinyourpocket.backend.domain.tourism.vo.Language
import kotlin.test.Test
import kotlin.test.assertEquals

class ListSpotsServiceTest {
    private class FakeSpotQuery : SpotQuery {
        var lastLanguage: Language? = null

        override fun findAllResolved(language: Language): List<SpotView> {
            lastLanguage = language
            return listOf(SAMPLE)
        }

        companion object {
            val SAMPLE =
                SpotView(
                    id = "kobe-port-tower",
                    name = "Kobe Port Tower",
                    genre = "landmark",
                    description = "Symbol of Kobe.",
                    latitude = 34.6826,
                    longitude = 135.1863,
                    businessHours = "9:00-23:00",
                    categoryLabel = "Landmark",
                    imageUrl = "https://example.com/x.webp",
                    rating = 4.5,
                )
        }
    }

    @Test
    fun `SpotQuery port へ言語を渡して SpotView を返す`() {
        val query = FakeSpotQuery()
        val result = ListSpotsService(query).listSpots(Language.EN)

        assertEquals(Language.EN, query.lastLanguage)
        assertEquals("Kobe Port Tower", result.single().name)
    }
}

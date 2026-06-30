package com.kobeinyourpocket.backend.application.tourism.query

import com.kobeinyourpocket.backend.domain.tourism.vo.Language
import com.kobeinyourpocket.backend.domain.tourism.vo.SpotId
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class ListReviewsServiceTest {
    private val reviewQuery = mockk<ReviewQuery>()
    private val service = ListReviewsService(reviewQuery)

    private val spotId = SpotId.of("kobe-port-tower")
    private val now = Instant.parse("2025-11-03T10:00:00Z")

    private val jaView =
        ReviewView(
            id = "00000000-0000-0000-0000-000000000001",
            spotId = spotId.value,
            rating = 5,
            comment = "素晴らしい",
            authorName = "Alice",
            authorIconUrl = null,
            createdAt = now,
            language = "ja",
        )

    @Test
    fun `spotId と language を ReviewQuery port に渡して ReviewView リストを返す`() {
        every { reviewQuery.findBySpot(spotId, Language.JA) } returns listOf(jaView)

        val result = service.listReviews(spotId, Language.JA)

        assertEquals(1, result.size)
        assertEquals("素晴らしい", result.single().comment)
        verify(exactly = 1) { reviewQuery.findBySpot(spotId, Language.JA) }
    }

    @Test
    fun `対象スポットにレビューがない場合は空リストを返す`() {
        every { reviewQuery.findBySpot(spotId, Language.EN) } returns emptyList()

        val result = service.listReviews(spotId, Language.EN)

        assertEquals(emptyList(), result)
    }
}

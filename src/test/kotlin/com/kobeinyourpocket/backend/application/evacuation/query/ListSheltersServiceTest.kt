package com.kobeinyourpocket.backend.application.evacuation.query

import com.kobeinyourpocket.backend.domain.common.localization.Language
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals

class ListSheltersServiceTest {
    private val jaView =
        ShelterView(
            id = "kobe-city-hall",
            name = "神戸市役所",
            address = "兵庫県神戸市中央区加納町6丁目5-1",
            latitude = 34.6826,
            longitude = 135.1863,
            type = "dual-use",
            facilityCategory = "government",
            imageUrl = "https://example.com/kobe-city-hall.webp",
            capacity = 500,
            accessible = true,
            externalUrl = "https://example.com/kobe-city-hall",
        )

    private val enView =
        jaView.copy(
            name = "Kobe City Hall",
            address = "6-5-1 Kanomachi, Chuo-ku, Kobe, Hyogo",
        )

    @Test
    fun `要求言語を ShelterQuery port に渡して解決済み ShelterView を返す`() {
        val shelterQuery = mockk<ShelterQuery>()
        every { shelterQuery.findAllResolved(Language.JA) } returns listOf(jaView)

        val result = ListSheltersService(shelterQuery).listShelters(Language.JA)

        assertEquals("神戸市役所", result.single().name)
        verify(exactly = 1) { shelterQuery.findAllResolved(Language.JA) }
    }

    @Test
    fun `ShelterQuery が返した ShelterView をそのまま返す`() {
        val shelterQuery = mockk<ShelterQuery>()
        every { shelterQuery.findAllResolved(Language.KO) } returns listOf(enView)

        val result = ListSheltersService(shelterQuery).listShelters(Language.KO)

        assertEquals(enView, result.single())
        verify(exactly = 1) { shelterQuery.findAllResolved(Language.KO) }
    }
}

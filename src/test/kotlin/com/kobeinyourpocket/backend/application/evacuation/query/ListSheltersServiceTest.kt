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
            id = "kobe-001",
            name = "東灘小学校",
            address = "神戸市東灘区深江北町2-4-1",
            latitude = 34.7248161,
            longitude = 135.2944292,
            type = "both",
            siting = "indoor",
            suitabilityLandslide = "suitable",
            suitabilityFlood = "suitable",
            suitabilityTsunami = "suitable",
            suitabilityLargeFire = "not-applicable",
            petAcceptance = "accepted",
            phoneNumber = "078-411-0556",
            note = null,
        )

    private val enView =
        jaView.copy(
            name = "Higashinada Elementary School",
            address = "2-4-1 Fukaekitamachi, Higashinada-ku, Kobe",
        )

    @Test
    fun `要求言語を ShelterQuery port に渡して解決済み ShelterView を返す`() {
        val shelterQuery = mockk<ShelterQuery>()
        every { shelterQuery.findAllResolved(Language.JA) } returns listOf(jaView)

        val result = ListSheltersService(shelterQuery).listShelters(Language.JA)

        assertEquals("東灘小学校", result.single().name)
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

package com.kobeinyourpocket.backend.application.evacuation.query

import com.kobeinyourpocket.backend.domain.common.localization.Language
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class GetShelterListServiceTest {
    private val shelterView =
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

    private val metadataView =
        ShelterDatasetMetadataView(
            source = "神戸市オープンデータ「指定緊急避難場所・指定避難所」(CC BY 4.0)",
            asOf = LocalDate.of(2025, 4, 2),
            updatedAt = Instant.parse("2025-04-02T00:00:00Z"),
        )

    @Test
    fun `ListSheltersService と GetShelterDatasetMetadataService を束ねて返す`() {
        val listSheltersService = mockk<ListSheltersService>()
        val getShelterDatasetMetadataService = mockk<GetShelterDatasetMetadataService>()
        every { listSheltersService.listShelters(Language.JA) } returns listOf(shelterView)
        every { getShelterDatasetMetadataService.getMetadata() } returns metadataView

        val result = GetShelterListService(listSheltersService, getShelterDatasetMetadataService).getShelterList(Language.JA)

        assertEquals(listOf(shelterView), result.shelters)
        assertEquals(metadataView, result.metadata)
        verify(exactly = 1) { listSheltersService.listShelters(Language.JA) }
        verify(exactly = 1) { getShelterDatasetMetadataService.getMetadata() }
    }
}

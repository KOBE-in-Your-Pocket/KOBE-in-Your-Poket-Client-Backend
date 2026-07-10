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

    private val metadataView =
        ShelterDatasetMetadataView(
            source = "神戸市オープンデータポータル「神戸市避難場所」(CC BY 2.1 JP)",
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

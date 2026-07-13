package com.kobeinyourpocket.backend.application.evacuation.query

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class GetShelterDatasetMetadataServiceTest {
    private val view =
        ShelterDatasetMetadataView(
            source = "神戸市オープンデータポータル「神戸市避難場所」(CC BY 2.1 JP)",
            asOf = LocalDate.of(2025, 4, 2),
            updatedAt = Instant.parse("2025-04-02T00:00:00Z"),
        )

    @Test
    fun `ShelterDatasetMetadataQuery port が返した値をそのまま返す`() {
        val shelterDatasetMetadataQuery = mockk<ShelterDatasetMetadataQuery>()
        every { shelterDatasetMetadataQuery.get() } returns view

        val result = GetShelterDatasetMetadataService(shelterDatasetMetadataQuery).getMetadata()

        assertEquals(view, result)
        verify(exactly = 1) { shelterDatasetMetadataQuery.get() }
    }
}

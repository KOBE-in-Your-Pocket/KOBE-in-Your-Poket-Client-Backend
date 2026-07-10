package com.kobeinyourpocket.backend.infrastructure.query.evacuation

import com.kobeinyourpocket.backend.infrastructure.persistence.evacuation.entity.ShelterDatasetMetadataEntity
import com.kobeinyourpocket.backend.infrastructure.persistence.evacuation.repository.ShelterDatasetMetadataJpaRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(ShelterDatasetMetadataQueryJpa::class)
class ShelterDatasetMetadataQueryJpaTest {
    @Autowired
    private lateinit var shelterDatasetMetadataJpaRepository: ShelterDatasetMetadataJpaRepository

    @Autowired
    private lateinit var shelterDatasetMetadataQuery: ShelterDatasetMetadataQueryJpa

    @Test
    fun `シングルトン行を ShelterDatasetMetadataView として返す`() {
        shelterDatasetMetadataJpaRepository.save(
            ShelterDatasetMetadataEntity(
                id = ShelterDatasetMetadataEntity.SINGLETON_ID,
                source = "神戸市オープンデータポータル「神戸市避難場所」(CC BY 2.1 JP)",
                asOf = LocalDate.of(2025, 4, 2),
                updatedAt = Instant.parse("2025-04-02T00:00:00Z"),
            ),
        )

        val result = shelterDatasetMetadataQuery.get()

        assertEquals("神戸市オープンデータポータル「神戸市避難場所」(CC BY 2.1 JP)", result.source)
        assertEquals(LocalDate.of(2025, 4, 2), result.asOf)
        assertEquals(Instant.parse("2025-04-02T00:00:00Z"), result.updatedAt)
    }
}

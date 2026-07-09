package com.kobeinyourpocket.backend.infrastructure.persistence.evacuation.impl

import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.model.EvacuationShelter
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.repository.ShelterRepository
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterCapacity
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterCoordinates
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterLocalization
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterLocalizations
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterMedia
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterType
import com.kobeinyourpocket.backend.domain.evacuation.shelterfacilitycategory.model.ShelterFacilityCategory
import com.kobeinyourpocket.backend.infrastructure.persistence.evacuation.repository.ShelterJpaRepository
import com.kobeinyourpocket.backend.infrastructure.persistence.evacuation.repository.ShelterLocalizationJpaRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(ShelterRepositoryImpl::class)
class ShelterRepositoryImplTest {
    @Autowired
    private lateinit var repository: ShelterRepository

    @Autowired
    private lateinit var shelterJpa: ShelterJpaRepository

    @Autowired
    private lateinit var localizationJpa: ShelterLocalizationJpaRepository

    private fun kobeCityHall(includeJa: Boolean = true): EvacuationShelter =
        EvacuationShelter.create(
            id = EvacuationShelter.Id.of("kobe-city-hall"),
            coordinates = ShelterCoordinates.of(34.6826, 135.1863),
            type = ShelterType.DUAL_USE,
            facilityCategory = ShelterFacilityCategory.GOVERNMENT,
            media = ShelterMedia("https://example.com/kobe-city-hall.webp"),
            accessible = true,
            localizations =
                ShelterLocalizations.of(
                    buildMap {
                        put(Language.EN, ShelterLocalization("Kobe City Hall", "6-5-1 Kanomachi, Chuo-ku, Kobe, Hyogo"))
                        if (includeJa) {
                            put(Language.JA, ShelterLocalization("神戸市役所", "兵庫県神戸市中央区加納町6丁目5-1"))
                        }
                    },
                ),
            capacity = ShelterCapacity(500),
            externalUrl = "https://example.com/kobe-city-hall",
        )

    @Test
    fun `save で shelter・shelter_localization が永続化される`() {
        repository.save(kobeCityHall())

        val entity = shelterJpa.findById("kobe-city-hall").orElseThrow()
        assertEquals(34.6826, entity.latitude)
        assertEquals(135.1863, entity.longitude)
        assertEquals("dual-use", entity.type)
        assertEquals("government", entity.facilityCategory)
        assertEquals("https://example.com/kobe-city-hall.webp", entity.imageUrl)
        assertEquals(true, entity.accessible)
        assertEquals(500, entity.capacity)
        assertEquals("https://example.com/kobe-city-hall", entity.externalUrl)

        val localizations = localizationJpa.findAll().filter { it.id.shelterId == "kobe-city-hall" }
        assertEquals(setOf("ja", "en"), localizations.map { it.id.language }.toSet())
    }

    @Test
    fun `capacity・externalUrl が無い場合は NULL で永続化される`() {
        val shelter =
            EvacuationShelter.create(
                id = EvacuationShelter.Id.of("minimal-shelter"),
                coordinates = ShelterCoordinates.of(34.0, 135.0),
                type = ShelterType.DESIGNATED_EMERGENCY_EVACUATION_SITE,
                facilityCategory = ShelterFacilityCategory.PARK,
                media = ShelterMedia("https://example.com/minimal.webp"),
                accessible = false,
                localizations = ShelterLocalizations.of(mapOf(Language.EN to ShelterLocalization("Minimal Park", "Somewhere"))),
            )

        repository.save(shelter)

        val entity = shelterJpa.findById("minimal-shelter").orElseThrow()
        assertNull(entity.capacity)
        assertNull(entity.externalUrl)
    }

    @Test
    fun `再 save で localization の削除が反映される（delete-then-insert）`() {
        repository.save(kobeCityHall(includeJa = true))
        repository.save(kobeCityHall(includeJa = false))

        val localizations = localizationJpa.findAll().filter { it.id.shelterId == "kobe-city-hall" }
        assertEquals(setOf("en"), localizations.map { it.id.language }.toSet())
    }
}

package com.kobeinyourpocket.backend.infrastructure.query.evacuation

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
import com.kobeinyourpocket.backend.infrastructure.persistence.evacuation.impl.ShelterRepositoryImpl
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
@Import(ShelterRepositoryImpl::class, ShelterQueryJpa::class)
class ShelterQueryJpaTest {
    @Autowired
    private lateinit var shelterRepository: ShelterRepository

    @Autowired
    private lateinit var shelterQuery: ShelterQueryJpa

    private val kobeCityHall =
        EvacuationShelter.create(
            id = EvacuationShelter.Id.of("kobe-city-hall"),
            coordinates = ShelterCoordinates.of(34.6826, 135.1863),
            type = ShelterType.DUAL_USE,
            facilityCategory = ShelterFacilityCategory.GOVERNMENT,
            media = ShelterMedia("https://example.com/kobe-city-hall.webp"),
            accessible = true,
            localizations =
                ShelterLocalizations.of(
                    mapOf(
                        Language.JA to ShelterLocalization("神戸市役所", "兵庫県神戸市中央区加納町6丁目5-1"),
                        Language.EN to ShelterLocalization("Kobe City Hall", "6-5-1 Kanomachi, Chuo-ku, Kobe, Hyogo"),
                    ),
                ),
            capacity = ShelterCapacity(500),
            externalUrl = "https://example.com/kobe-city-hall",
        )

    private val minimalShelter =
        EvacuationShelter.create(
            id = EvacuationShelter.Id.of("minimal-shelter"),
            coordinates = ShelterCoordinates.of(34.0, 135.0),
            type = ShelterType.DESIGNATED_EMERGENCY_EVACUATION_SITE,
            facilityCategory = ShelterFacilityCategory.PARK,
            media = ShelterMedia("https://example.com/minimal.webp"),
            accessible = false,
            localizations = ShelterLocalizations.of(mapOf(Language.EN to ShelterLocalization("Minimal Park", "Somewhere"))),
        )

    @Test
    fun `要求言語で解決した ShelterView を返す`() {
        shelterRepository.save(kobeCityHall)

        val result = shelterQuery.findAllResolved(Language.JA).single()

        assertEquals("kobe-city-hall", result.id)
        assertEquals("神戸市役所", result.name)
        assertEquals("兵庫県神戸市中央区加納町6丁目5-1", result.address)
        assertEquals(34.6826, result.latitude)
        assertEquals(135.1863, result.longitude)
        assertEquals("both", result.type)
        assertEquals("government", result.facilityCategory)
        assertEquals("https://example.com/kobe-city-hall.webp", result.imageUrl)
        assertEquals(500, result.capacity)
        assertEquals(true, result.accessible)
        assertEquals("https://example.com/kobe-city-hall", result.externalUrl)
    }

    @Test
    fun `要求言語のローカライズが無ければ en へフォールバックする`() {
        shelterRepository.save(minimalShelter)

        val result = shelterQuery.findAllResolved(Language.KO).single()

        assertEquals("Minimal Park", result.name)
        assertEquals("Somewhere", result.address)
    }

    @Test
    fun `capacity・externalUrl が無い避難所は null で返す`() {
        shelterRepository.save(minimalShelter)

        val result = shelterQuery.findAllResolved(Language.EN).single()

        assertNull(result.capacity)
        assertNull(result.externalUrl)
    }

    @Test
    fun `全件を id 順で返す`() {
        shelterRepository.save(minimalShelter)
        shelterRepository.save(kobeCityHall)

        val result = shelterQuery.findAllResolved(Language.EN)

        assertEquals(listOf("kobe-city-hall", "minimal-shelter"), result.map { it.id })
    }
}

package com.kobeinyourpocket.backend.domain.evacuation

import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.model.EvacuationShelter
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.repository.ShelterRepository
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterCoordinates
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterLocalization
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterLocalizations
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterMedia
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterType
import com.kobeinyourpocket.backend.domain.evacuation.shelterfacilitycategory.model.ShelterFacilityCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** ShelterRepository write port の契約を Fake で検証する。 */
class ShelterRepositoryPortTest {
    private class FakeShelterRepository : ShelterRepository {
        private val store = linkedMapOf<EvacuationShelter.Id, EvacuationShelter>()

        override fun save(shelter: EvacuationShelter): EvacuationShelter {
            store[shelter.id] = shelter
            return shelter
        }

        override fun existsById(id: EvacuationShelter.Id): Boolean = store.containsKey(id)

        override fun deleteById(id: EvacuationShelter.Id) {
            store.remove(id)
        }

        fun get(id: EvacuationShelter.Id): EvacuationShelter? = store[id]
    }

    private fun shelter(id: String): EvacuationShelter =
        EvacuationShelter.create(
            id = EvacuationShelter.Id.of(id),
            coordinates = ShelterCoordinates.of(34.6826, 135.1863),
            type = ShelterType.DUAL_USE,
            facilityCategory = ShelterFacilityCategory.GOVERNMENT,
            media = ShelterMedia("https://example.com/$id.webp"),
            accessible = true,
            localizations =
                ShelterLocalizations.of(
                    mapOf(Language.EN to ShelterLocalization(name = "Kobe City Hall", address = "6-5-1 Kanomachi")),
                ),
        )

    @Test
    fun `save した集約を取得できる`() {
        val repository = FakeShelterRepository()
        val shelter =
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
                            Language.JA to
                                ShelterLocalization(
                                    name = "神戸市役所",
                                    address = "兵庫県神戸市中央区加納町6丁目5-1",
                                ),
                            Language.EN to
                                ShelterLocalization(
                                    name = "Kobe City Hall",
                                    address = "6-5-1 Kanomachi, Chuo-ku, Kobe, Hyogo",
                                ),
                        ),
                    ),
            )

        repository.save(shelter)

        assertEquals(shelter, repository.get(EvacuationShelter.Id.of("kobe-city-hall")))
    }

    @Test
    fun `existsById は save 済みかどうかを返す`() {
        val repository = FakeShelterRepository()
        repository.save(shelter("kobe-city-hall"))

        assertTrue(repository.existsById(EvacuationShelter.Id.of("kobe-city-hall")))
        assertFalse(repository.existsById(EvacuationShelter.Id.of("unknown-shelter")))
    }

    @Test
    fun `deleteById した集約は取得できなくなる`() {
        val repository = FakeShelterRepository()
        val id = EvacuationShelter.Id.of("kobe-city-hall")
        repository.save(shelter("kobe-city-hall"))

        repository.deleteById(id)

        assertNull(repository.get(id))
        assertFalse(repository.existsById(id))
    }
}

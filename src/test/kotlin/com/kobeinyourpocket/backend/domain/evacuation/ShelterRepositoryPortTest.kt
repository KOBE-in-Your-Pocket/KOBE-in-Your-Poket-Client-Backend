package com.kobeinyourpocket.backend.domain.evacuation

import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.model.EvacuationShelter
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.repository.ShelterRepository
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.PetAcceptance
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterCoordinates
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterLocalization
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterLocalizations
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterSiting
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterSuitabilities
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterSuitability
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterType
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

    private fun shelter(
        id: String,
        localizations: ShelterLocalizations =
            ShelterLocalizations.of(
                mapOf(Language.EN to ShelterLocalization(name = "Higashinada Elementary School", address = "2-4-1 Fukaekitamachi")),
            ),
    ): EvacuationShelter =
        EvacuationShelter.create(
            id = EvacuationShelter.Id.of(id),
            coordinates = ShelterCoordinates.of(34.7248161, 135.2944292),
            type = ShelterType.DUAL_USE,
            siting = ShelterSiting.INDOOR,
            suitabilities =
                ShelterSuitabilities.of(
                    landslide = ShelterSuitability.SUITABLE,
                    flood = ShelterSuitability.SUITABLE,
                    tsunami = ShelterSuitability.SUITABLE,
                    largeFire = ShelterSuitability.NOT_APPLICABLE,
                ),
            petAcceptance = PetAcceptance.ACCEPTED,
            localizations = localizations,
            phoneNumber = "078-411-0556",
        )

    @Test
    fun `save した集約を取得できる`() {
        val repository = FakeShelterRepository()
        val shelter =
            shelter(
                "kobe-001",
                ShelterLocalizations.of(
                    mapOf(
                        Language.JA to
                            ShelterLocalization(
                                name = "東灘小学校",
                                address = "神戸市東灘区深江北町2-4-1",
                            ),
                        Language.EN to
                            ShelterLocalization(
                                name = "Higashinada Elementary School",
                                address = "2-4-1 Fukaekitamachi, Higashinada-ku, Kobe",
                            ),
                    ),
                ),
            )

        repository.save(shelter)

        assertEquals(shelter, repository.get(EvacuationShelter.Id.of("kobe-001")))
    }

    @Test
    fun `existsById は save 済みかどうかを返す`() {
        val repository = FakeShelterRepository()
        repository.save(shelter("kobe-001"))

        assertTrue(repository.existsById(EvacuationShelter.Id.of("kobe-001")))
        assertFalse(repository.existsById(EvacuationShelter.Id.of("kobe-999")))
    }

    @Test
    fun `deleteById した集約は取得できなくなる`() {
        val repository = FakeShelterRepository()
        val id = EvacuationShelter.Id.of("kobe-001")
        repository.save(shelter("kobe-001"))

        repository.deleteById(id)

        assertNull(repository.get(id))
        assertFalse(repository.existsById(id))
    }
}

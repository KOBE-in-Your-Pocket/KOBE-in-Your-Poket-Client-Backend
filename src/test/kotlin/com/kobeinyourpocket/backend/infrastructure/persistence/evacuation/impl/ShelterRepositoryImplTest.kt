package com.kobeinyourpocket.backend.infrastructure.persistence.evacuation.impl

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

    private fun higashinadaElementary(includeJa: Boolean = true): EvacuationShelter =
        EvacuationShelter.create(
            id = EvacuationShelter.Id.of("kobe-001"),
            coordinates = ShelterCoordinates.of(34.7248161, 135.2944292),
            type = ShelterType.DUAL_USE,
            siting = ShelterSiting.INDOOR,
            suitabilities =
                ShelterSuitabilities.of(
                    landslide = ShelterSuitability.SUITABLE,
                    flood = ShelterSuitability.CONDITIONAL,
                    tsunami = ShelterSuitability.UNSUITABLE,
                    largeFire = ShelterSuitability.NOT_APPLICABLE,
                ),
            petAcceptance = PetAcceptance.ACCEPTED,
            localizations =
                ShelterLocalizations.of(
                    buildMap {
                        put(
                            Language.EN,
                            ShelterLocalization("Higashinada Elementary School", "2-4-1 Fukaekitamachi, Higashinada-ku, Kobe"),
                        )
                        if (includeJa) {
                            put(
                                Language.JA,
                                ShelterLocalization("東灘小学校", "神戸市東灘区深江北町2-4-1", note = "《洪水時》別の避難場所へ避難"),
                            )
                        }
                    },
                ),
            phoneNumber = "078-411-0556",
        )

    @Test
    fun `save で shelter・shelter_localization が永続化される`() {
        repository.save(higashinadaElementary())

        val entity = shelterJpa.findById("kobe-001").orElseThrow()
        assertEquals(34.7248161, entity.latitude)
        assertEquals(135.2944292, entity.longitude)
        assertEquals("both", entity.type)
        assertEquals("indoor", entity.siting)
        assertEquals("suitable", entity.suitabilityLandslide)
        assertEquals("conditional", entity.suitabilityFlood)
        assertEquals("unsuitable", entity.suitabilityTsunami)
        assertEquals("not-applicable", entity.suitabilityLargeFire)
        assertEquals("accepted", entity.petAcceptance)
        assertEquals("078-411-0556", entity.phoneNumber)

        val localizations = localizationJpa.findAll().filter { it.id.shelterId == "kobe-001" }
        assertEquals(setOf("ja", "en"), localizations.map { it.id.language }.toSet())
        assertEquals(
            "《洪水時》別の避難場所へ避難",
            localizations.single { it.id.language == "ja" }.note,
        )
    }

    @Test
    fun `電話番号・備考が無い場合は NULL で永続化される`() {
        // 元データでは屋外の緊急避難場所 88 件が電話番号を持たず、備考も大半が空。
        val shelter =
            EvacuationShelter.create(
                id = EvacuationShelter.Id.of("kobe-099"),
                coordinates = ShelterCoordinates.of(34.0, 135.0),
                type = ShelterType.DESIGNATED_EMERGENCY_EVACUATION_SITE,
                siting = ShelterSiting.OUTDOOR,
                suitabilities =
                    ShelterSuitabilities.of(
                        landslide = ShelterSuitability.NOT_APPLICABLE,
                        flood = ShelterSuitability.NOT_APPLICABLE,
                        tsunami = ShelterSuitability.SUITABLE,
                        largeFire = ShelterSuitability.SUITABLE,
                    ),
                petAcceptance = PetAcceptance.UNDER_CONSIDERATION,
                localizations = ShelterLocalizations.of(mapOf(Language.EN to ShelterLocalization("Honjo Park", "Somewhere"))),
            )

        repository.save(shelter)

        val entity = shelterJpa.findById("kobe-099").orElseThrow()
        assertNull(entity.phoneNumber)
        assertNull(localizationJpa.findAll().single { it.id.shelterId == "kobe-099" }.note)
    }

    @Test
    fun `再 save で localization の削除が反映される（delete-then-insert）`() {
        repository.save(higashinadaElementary(includeJa = true))
        repository.save(higashinadaElementary(includeJa = false))

        val localizations = localizationJpa.findAll().filter { it.id.shelterId == "kobe-001" }
        assertEquals(setOf("en"), localizations.map { it.id.language }.toSet())
    }
}

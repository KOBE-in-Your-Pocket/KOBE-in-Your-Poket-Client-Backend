package com.kobeinyourpocket.backend.infrastructure.query.evacuation

import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.model.EvacuationShelter
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.repository.ShelterRepository
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.DisasterType
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.PetAcceptance
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterCoordinates
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterLocalization
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterLocalizations
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterSiting
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterSuitabilities
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterSuitability
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterType
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

    /** 屋内・備考つき。屋外との違いを出すため適否も種別ごとに散らしてある。 */
    private val higashinadaElementary =
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
                    mapOf(
                        Language.JA to
                            ShelterLocalization("東灘小学校", "神戸市東灘区深江北町2-4-1", note = "《洪水時》別の避難場所へ避難"),
                        Language.EN to
                            ShelterLocalization(
                                "Higashinada Elementary School",
                                "2-4-1 Fukaekitamachi, Higashinada-ku, Kobe",
                                note = "In case of flood, evacuate to another site.",
                            ),
                    ),
                ),
            phoneNumber = "078-411-0556",
        )

    /** 屋外・電話番号と備考なし。元データの屋外 88 件に相当する。 */
    private val honjoPark =
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
            petAcceptance = PetAcceptance.NOT_ACCEPTED,
            localizations = ShelterLocalizations.of(mapOf(Language.EN to ShelterLocalization("Honjo Park", "Somewhere"))),
        )

    @Test
    fun `要求言語で解決した ShelterView を返す`() {
        shelterRepository.save(higashinadaElementary)

        val result = shelterQuery.findAllResolved(Language.JA, emptySet()).single()

        assertEquals("kobe-001", result.id)
        assertEquals("東灘小学校", result.name)
        assertEquals("神戸市東灘区深江北町2-4-1", result.address)
        assertEquals("《洪水時》別の避難場所へ避難", result.note)
        assertEquals(34.7248161, result.latitude)
        assertEquals(135.2944292, result.longitude)
        assertEquals("both", result.type)
        assertEquals("indoor", result.siting)
        assertEquals("suitable", result.suitabilityLandslide)
        assertEquals("conditional", result.suitabilityFlood)
        assertEquals("unsuitable", result.suitabilityTsunami)
        assertEquals("not-applicable", result.suitabilityLargeFire)
        assertEquals("accepted", result.petAcceptance)
        assertEquals("078-411-0556", result.phoneNumber)
    }

    @Test
    fun `要求言語のローカライズが無ければ en へフォールバックする`() {
        shelterRepository.save(honjoPark)

        val result = shelterQuery.findAllResolved(Language.KO, emptySet()).single()

        assertEquals("Honjo Park", result.name)
        assertEquals("Somewhere", result.address)
    }

    @Test
    fun `電話番号・備考が無い避難所は null で返す`() {
        shelterRepository.save(honjoPark)

        val result = shelterQuery.findAllResolved(Language.EN, emptySet()).single()

        assertNull(result.phoneNumber)
        assertNull(result.note)
    }

    @Test
    fun `備考も要求言語で解決する`() {
        // 備考は避難の判断に関わるため、名称・住所と同じく言語別に解決する必要がある。
        shelterRepository.save(higashinadaElementary)

        val result = shelterQuery.findAllResolved(Language.EN, emptySet()).single()

        assertEquals("In case of flood, evacuate to another site.", result.note)
    }

    @Test
    fun `全件を id 順で返す`() {
        shelterRepository.save(honjoPark)
        shelterRepository.save(higashinadaElementary)

        val result = shelterQuery.findAllResolved(Language.EN, emptySet())

        assertEquals(listOf("kobe-001", "kobe-099"), result.map { it.id })
    }

    private fun idsSuitableFor(vararg types: DisasterType): List<String> {
        shelterRepository.save(higashinadaElementary)
        shelterRepository.save(honjoPark)
        return shelterQuery.findAllResolved(Language.EN, types.toSet()).map { it.id }
    }

    @Test
    fun `災害種別を指定すると、その種別で ○ の避難所だけを返す`() {
        assertEquals(listOf("kobe-001"), idsSuitableFor(DisasterType.LANDSLIDE))
        assertEquals(listOf("kobe-099"), idsSuitableFor(DisasterType.TSUNAMI))
    }

    @Test
    fun `△（条件付き）と ×・対象外は絞り込みに含めない`() {
        // 東灘小は洪水 △、本庄公園は洪水 対象外。
        assertEquals(emptyList(), idsSuitableFor(DisasterType.FLOOD))
    }

    @Test
    fun `複数の種別はいずれかで ○ なら含める（OR）`() {
        assertEquals(listOf("kobe-001", "kobe-099"), idsSuitableFor(DisasterType.LANDSLIDE, DisasterType.TSUNAMI))
        assertEquals(listOf("kobe-099"), idsSuitableFor(DisasterType.FLOOD, DisasterType.LARGE_FIRE))
    }

    @Test
    fun `種別を指定しなければ全件を返す`() {
        assertEquals(listOf("kobe-001", "kobe-099"), idsSuitableFor())
    }
}

package com.kobeinyourpocket.backend.domain.evacuation

import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.model.EvacuationShelter
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.DisasterType
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
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

private fun localizationsOf(vararg languages: Language) =
    ShelterLocalizations.of(
        languages.associateWith {
            ShelterLocalization(
                name = "name-${it.code}",
                address = "address-${it.code}",
            )
        },
    )

/** 全種別が suitable の適否。個別に変えたいテストだけ copy する。 */
private fun allSuitable() =
    ShelterSuitabilities.of(
        landslide = ShelterSuitability.SUITABLE,
        flood = ShelterSuitability.SUITABLE,
        tsunami = ShelterSuitability.SUITABLE,
        largeFire = ShelterSuitability.SUITABLE,
    )

class EvacuationShelterIdTest {
    @Test
    fun `slug 形式の ID を生成できる`() {
        assertEquals("kobe-city-hall", EvacuationShelter.Id.of("kobe-city-hall").value)
    }

    @Test
    fun `前後空白は trim される`() {
        assertEquals("kobe-city-hall", EvacuationShelter.Id.of("  kobe-city-hall  ").value)
    }

    @Test
    fun `空文字は拒否する`() {
        assertFailsWith<IllegalArgumentException> {
            EvacuationShelter.Id.of("   ")
        }
    }
}

class ShelterCoordinatesTest {
    @Test
    fun `有効な緯度経度で生成できる`() {
        val coordinates = ShelterCoordinates.of(latitude = 34.6826, longitude = 135.1863)

        assertEquals(34.6826, coordinates.latitude)
        assertEquals(135.1863, coordinates.longitude)
    }

    @Test
    fun `緯度が範囲外なら拒否する`() {
        assertFailsWith<IllegalArgumentException> {
            ShelterCoordinates.of(latitude = 91.0, longitude = 135.0)
        }
    }

    @Test
    fun `経度が範囲外なら拒否する`() {
        assertFailsWith<IllegalArgumentException> {
            ShelterCoordinates.of(latitude = 34.0, longitude = -181.0)
        }
    }
}

class ShelterTypeTest {
    @Test
    fun `災害対策基本法の区分に沿った wireValue を持つ`() {
        assertEquals(
            "emergency",
            ShelterType.DESIGNATED_EMERGENCY_EVACUATION_SITE.wireValue,
        )
        assertEquals(
            "designated",
            ShelterType.DESIGNATED_EVACUATION_SHELTER.wireValue,
        )
        assertEquals("both", ShelterType.DUAL_USE.wireValue)
    }

    @Test
    fun `wireValue から解決でき trim と lowercase を正規化する`() {
        assertEquals(
            ShelterType.DESIGNATED_EMERGENCY_EVACUATION_SITE,
            ShelterType.of("emergency"),
        )
        assertEquals(
            ShelterType.DESIGNATED_EVACUATION_SHELTER,
            ShelterType.of("  DESIGNATED  "),
        )
        assertEquals(ShelterType.DUAL_USE, ShelterType.of("both"))
    }

    @Test
    fun `未対応値は null を返す`() {
        assertNull(ShelterType.of("unknown"))
        assertNull(ShelterType.of(""))
    }

    /**
     * #162 以前の wire 値（災対法の用語をそのまま slug 化した長い形）は受け付けない。
     *
     * V12 で DB の値も移行済みのため、これらが解決できてしまうと移行漏れを見逃す。
     */
    @Test
    fun `移行前の旧 wireValue は解決しない`() {
        assertNull(ShelterType.of("designated-emergency-evacuation-site"))
        assertNull(ShelterType.of("designated-evacuation-shelter"))
        assertNull(ShelterType.of("dual-use"))
    }
}

class DisasterTypeTest {
    @Test
    fun `元データの 4 種だけを持つ`() {
        // 高潮・内水氾濫などは神戸市オープンデータに列が無い。持っていない種別を
        // 並べると「対応していない」と「データが無い」の区別が付かなくなる（#180）。
        assertEquals(
            listOf("landslide", "flood", "tsunami", "large-fire"),
            DisasterType.entries.map(DisasterType::wireValue),
        )
    }

    @Test
    fun `オープンデータの列名と対応が付く`() {
        assertEquals("土砂災害", DisasterType.LANDSLIDE.sourceColumn)
        assertEquals("大火事", DisasterType.LARGE_FIRE.sourceColumn)
    }

    @Test
    fun `wireValue から解決でき trim と lowercase を正規化する`() {
        assertEquals(DisasterType.FLOOD, DisasterType.of("  FLOOD "))
        assertEquals(DisasterType.LARGE_FIRE, DisasterType.of("large-fire"))
    }

    @Test
    fun `未対応値は null を返す`() {
        assertNull(DisasterType.of("storm-surge"))
        assertNull(DisasterType.of(""))
    }
}

class ShelterSuitabilityTest {
    @Test
    fun `元データの 5 値を 4 値に寄せた wireValue を持つ`() {
        assertEquals("suitable", ShelterSuitability.SUITABLE.wireValue)
        assertEquals("conditional", ShelterSuitability.CONDITIONAL.wireValue)
        assertEquals("unsuitable", ShelterSuitability.UNSUITABLE.wireValue)
        assertEquals("not-applicable", ShelterSuitability.NOT_APPLICABLE.wireValue)
    }

    @Test
    fun `条件付きは利用できるとは別の値`() {
        // △ を ○ に丸めると、条件付きの避難所を「使える」と表示してしまう。
        assertNotEquals(ShelterSuitability.SUITABLE, ShelterSuitability.CONDITIONAL)
    }

    @Test
    fun `未対応値は null を返す`() {
        assertNull(ShelterSuitability.of("maybe"))
        assertNull(ShelterSuitability.of(""))
    }
}

class ShelterSuitabilitiesTest {
    @Test
    fun `全災害種別の適否を引ける`() {
        val suitabilities =
            ShelterSuitabilities.of(
                landslide = ShelterSuitability.SUITABLE,
                flood = ShelterSuitability.CONDITIONAL,
                tsunami = ShelterSuitability.UNSUITABLE,
                largeFire = ShelterSuitability.NOT_APPLICABLE,
            )

        assertEquals(ShelterSuitability.SUITABLE, suitabilities.of(DisasterType.LANDSLIDE))
        assertEquals(ShelterSuitability.CONDITIONAL, suitabilities.of(DisasterType.FLOOD))
        assertEquals(ShelterSuitability.UNSUITABLE, suitabilities.of(DisasterType.TSUNAMI))
        assertEquals(ShelterSuitability.NOT_APPLICABLE, suitabilities.of(DisasterType.LARGE_FIRE))
    }

    @Test
    fun `種別が欠けていたら拒否する`() {
        // 欠けた種別を「不明」として扱うと、表示側の分岐を書き忘れたときに危険側へ倒れる。
        assertFailsWith<IllegalArgumentException> {
            ShelterSuitabilities.of(
                mapOf(
                    DisasterType.LANDSLIDE to ShelterSuitability.SUITABLE,
                    DisasterType.FLOOD to ShelterSuitability.SUITABLE,
                ),
            )
        }
    }

    @Test
    fun `渡した Map を後から変えても影響を受けない`() {
        val source =
            mutableMapOf(
                DisasterType.LANDSLIDE to ShelterSuitability.SUITABLE,
                DisasterType.FLOOD to ShelterSuitability.SUITABLE,
                DisasterType.TSUNAMI to ShelterSuitability.SUITABLE,
                DisasterType.LARGE_FIRE to ShelterSuitability.SUITABLE,
            )
        val suitabilities = ShelterSuitabilities.of(source)

        source[DisasterType.TSUNAMI] = ShelterSuitability.UNSUITABLE

        assertEquals(ShelterSuitability.SUITABLE, suitabilities.of(DisasterType.TSUNAMI))
    }
}

class ShelterSitingTest {
    @Test
    fun `屋内と屋外の 2 値`() {
        assertEquals("indoor", ShelterSiting.INDOOR.wireValue)
        assertEquals("outdoor", ShelterSiting.OUTDOOR.wireValue)
        assertEquals(2, ShelterSiting.entries.size)
    }

    @Test
    fun `未対応値は null を返す`() {
        assertNull(ShelterSiting.of("underground"))
        assertNull(ShelterSiting.of(""))
    }
}

class PetAcceptanceTest {
    @Test
    fun `調整中を含む 3 値`() {
        // 元データに「調整中」が 38 件あり、可否の 2 値には潰せない。
        assertEquals("accepted", PetAcceptance.ACCEPTED.wireValue)
        assertEquals("not-accepted", PetAcceptance.NOT_ACCEPTED.wireValue)
        assertEquals("under-consideration", PetAcceptance.UNDER_CONSIDERATION.wireValue)
        assertEquals(3, PetAcceptance.entries.size)
    }

    @Test
    fun `未対応値は null を返す`() {
        assertNull(PetAcceptance.of("maybe"))
        assertNull(PetAcceptance.of(""))
    }
}

class ShelterLocalizationTest {
    @Test
    fun `name と address を保持する`() {
        val localization =
            ShelterLocalization(
                name = "東灘小学校",
                address = "神戸市東灘区深江北町2-4-1",
            )

        assertEquals("東灘小学校", localization.name)
        assertEquals("神戸市東灘区深江北町2-4-1", localization.address)
        assertNull(localization.note)
    }

    @Test
    fun `note は任意`() {
        val localization =
            ShelterLocalization(
                name = "name",
                address = "address",
                note = "《土砂災害時》正門が土砂災害警戒区域内にあるので注意、早めに避難",
            )

        assertEquals("《土砂災害時》正門が土砂災害警戒区域内にあるので注意、早めに避難", localization.note)
    }

    @Test
    fun `name が空なら拒否する`() {
        assertFailsWith<IllegalArgumentException> {
            ShelterLocalization(name = "  ", address = "address")
        }
    }

    @Test
    fun `address が空なら拒否する`() {
        assertFailsWith<IllegalArgumentException> {
            ShelterLocalization(name = "name", address = "  ")
        }
    }

    @Test
    fun `note が空文字なら拒否する`() {
        // 「備考が無い」は null で表す。空文字が混ざると表示側で空欄が出る。
        assertFailsWith<IllegalArgumentException> {
            ShelterLocalization(name = "name", address = "address", note = "  ")
        }
    }
}

class ShelterLocalizationsTest {
    @Test
    fun `要求言語のローカライズを返す`() {
        val localizations = localizationsOf(Language.JA, Language.EN)

        assertEquals("name-ja", localizations.resolve(Language.JA).name)
    }

    @Test
    fun `要求言語が無ければ en へフォールバックする`() {
        val localizations = localizationsOf(Language.EN)

        assertEquals("name-en", localizations.resolve(Language.KO).name)
    }

    @Test
    fun `フォールバック先は en`() {
        assertEquals(Language.EN, ShelterLocalizations.FALLBACK)
    }

    @Test
    fun `フォールバック言語 en を含まない場合は拒否する`() {
        assertFailsWith<IllegalArgumentException> {
            localizationsOf(Language.JA)
        }
    }
}

class EvacuationShelterTest {
    @Test
    fun `集約ルートを生成し localizations を所有する`() {
        val shelter =
            EvacuationShelter.create(
                id = EvacuationShelter.Id.of("kobe-001"),
                coordinates = ShelterCoordinates.of(34.7248161, 135.2944292),
                type = ShelterType.DUAL_USE,
                siting = ShelterSiting.INDOOR,
                suitabilities = allSuitable(),
                petAcceptance = PetAcceptance.ACCEPTED,
                localizations = localizationsOf(Language.JA, Language.EN),
                phoneNumber = "078-411-0556",
            )

        assertEquals("kobe-001", shelter.id.value)
        assertEquals(ShelterType.DUAL_USE, shelter.type)
        assertEquals(ShelterSiting.INDOOR, shelter.siting)
        assertEquals(PetAcceptance.ACCEPTED, shelter.petAcceptance)
        assertEquals("name-ja", shelter.localizations.resolve(Language.JA).name)
        assertEquals("078-411-0556", shelter.phoneNumber)
    }

    @Test
    fun `phoneNumber は任意で、空文字は null に寄せる`() {
        // 元データでは屋外の緊急避難場所 88 件が電話番号を持たない。
        val shelter =
            EvacuationShelter.create(
                id = EvacuationShelter.Id.of("kobe-099"),
                coordinates = ShelterCoordinates.of(34.7050, 135.1900),
                type = ShelterType.DESIGNATED_EMERGENCY_EVACUATION_SITE,
                siting = ShelterSiting.OUTDOOR,
                suitabilities = allSuitable(),
                petAcceptance = PetAcceptance.UNDER_CONSIDERATION,
                localizations = localizationsOf(Language.EN),
                phoneNumber = "   ",
            )

        assertNull(shelter.phoneNumber)
    }

    @Test
    fun `災害種別ごとの適否を引ける`() {
        val shelter =
            EvacuationShelter.create(
                id = EvacuationShelter.Id.of("kobe-002"),
                coordinates = ShelterCoordinates.of(34.7210202, 135.2886997),
                type = ShelterType.DUAL_USE,
                siting = ShelterSiting.INDOOR,
                suitabilities =
                    ShelterSuitabilities.of(
                        landslide = ShelterSuitability.CONDITIONAL,
                        flood = ShelterSuitability.SUITABLE,
                        tsunami = ShelterSuitability.UNSUITABLE,
                        largeFire = ShelterSuitability.NOT_APPLICABLE,
                    ),
                petAcceptance = PetAcceptance.ACCEPTED,
                localizations = localizationsOf(Language.EN),
            )

        assertEquals(ShelterSuitability.CONDITIONAL, shelter.suitabilities.of(DisasterType.LANDSLIDE))
        assertEquals(ShelterSuitability.UNSUITABLE, shelter.suitabilities.of(DisasterType.TSUNAMI))
    }
}

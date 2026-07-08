package com.kobeinyourpocket.backend.domain.evacuation

import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.model.EvacuationShelter
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterCapacity
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterCoordinates
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterExternalUrl
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterLocalization
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterLocalizations
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterMedia
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterType
import com.kobeinyourpocket.backend.domain.evacuation.shelterfacilitycategory.model.ShelterFacilityCategory
import com.kobeinyourpocket.backend.domain.common.localization.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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
            "designated-emergency-evacuation-site",
            ShelterType.DESIGNATED_EMERGENCY_EVACUATION_SITE.wireValue,
        )
        assertEquals(
            "designated-evacuation-shelter",
            ShelterType.DESIGNATED_EVACUATION_SHELTER.wireValue,
        )
        assertEquals("dual-use", ShelterType.DUAL_USE.wireValue)
    }

    @Test
    fun `wireValue から解決でき trim と lowercase を正規化する`() {
        assertEquals(
            ShelterType.DESIGNATED_EMERGENCY_EVACUATION_SITE,
            ShelterType.of("designated-emergency-evacuation-site"),
        )
        assertEquals(
            ShelterType.DESIGNATED_EVACUATION_SHELTER,
            ShelterType.of("  DESIGNATED-EVACUATION-SHELTER  "),
        )
        assertEquals(ShelterType.DUAL_USE, ShelterType.of("dual-use"))
    }

    @Test
    fun `未対応値は null を返す`() {
        assertNull(ShelterType.of("unknown"))
        assertNull(ShelterType.of(""))
        assertNull(ShelterType.of("emergency"))
        assertNull(ShelterType.of("designated"))
        assertNull(ShelterType.of("both"))
    }
}

class ShelterFacilityCategoryTest {
    @Test
    fun `Client と同じ wireValue を持つ`() {
        assertEquals("government", ShelterFacilityCategory.GOVERNMENT.wireValue)
        assertEquals("school", ShelterFacilityCategory.SCHOOL.wireValue)
        assertEquals("park", ShelterFacilityCategory.PARK.wireValue)
        assertEquals("gymnasium", ShelterFacilityCategory.GYMNASIUM.wireValue)
    }

    @Test
    fun `code から生成でき trim と lowercase を正規化する`() {
        assertEquals(ShelterFacilityCategory.GOVERNMENT, ShelterFacilityCategory.of("government"))
        assertEquals(ShelterFacilityCategory.SCHOOL, ShelterFacilityCategory.of("  SCHOOL  "))
    }

    @Test
    fun `未知の code も生成できる`() {
        assertEquals("hospital", ShelterFacilityCategory.of("hospital").wireValue)
    }

    @Test
    fun `空文字の code は拒否する`() {
        assertFailsWith<IllegalArgumentException> {
            ShelterFacilityCategory.of("   ")
        }
    }
}

class ShelterMediaTest {
    @Test
    fun `imageUrl を保持する`() {
        val media = ShelterMedia("https://example.com/shelter.webp")

        assertEquals("https://example.com/shelter.webp", media.imageUrl)
    }

    @Test
    fun `空文字は拒否する`() {
        assertFailsWith<IllegalArgumentException> {
            ShelterMedia("   ")
        }
    }
}

class ShelterCapacityTest {
    @Test
    fun `正の整数を保持する`() {
        assertEquals(500, ShelterCapacity(500).value)
    }

    @Test
    fun `0 以下は拒否する`() {
        assertFailsWith<IllegalArgumentException> {
            ShelterCapacity(0)
        }
    }
}

class ShelterLocalizationTest {
    @Test
    fun `name と address を保持する`() {
        val localization =
            ShelterLocalization(
                name = "神戸市役所",
                address = "兵庫県神戸市中央区加納町6丁目5-1",
            )

        assertEquals("神戸市役所", localization.name)
        assertEquals("兵庫県神戸市中央区加納町6丁目5-1", localization.address)
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
                id = EvacuationShelter.Id.of("kobe-city-hall"),
                coordinates = ShelterCoordinates.of(34.6826, 135.1863),
                type = ShelterType.DUAL_USE,
                facilityCategory = ShelterFacilityCategory.GOVERNMENT,
                media = ShelterMedia("https://example.com/kobe-city-hall.webp"),
                accessible = true,
                localizations = localizationsOf(Language.JA, Language.EN),
            )

        assertEquals("kobe-city-hall", shelter.id.value)
        assertEquals(ShelterType.DUAL_USE, shelter.type)
        assertEquals("name-ja", shelter.localizations.resolve(Language.JA).name)
        assertNull(shelter.capacity)
        assertNull(shelter.externalUrl)
    }

    @Test
    fun `capacity と externalUrl は任意`() {
        val shelter =
            EvacuationShelter.create(
                id = EvacuationShelter.Id.of("nunobiki-park"),
                coordinates = ShelterCoordinates.of(34.7050, 135.1900),
                type = ShelterType.DESIGNATED_EMERGENCY_EVACUATION_SITE,
                facilityCategory = ShelterFacilityCategory.PARK,
                media = ShelterMedia("https://example.com/nunobiki-park.webp"),
                accessible = false,
                localizations = localizationsOf(Language.EN),
                capacity = ShelterCapacity(1200),
                externalUrl = "https://example.com/evacuation-info",
            )

        assertEquals(1200, shelter.capacity?.value)
        assertEquals("https://example.com/evacuation-info", shelter.externalUrl?.value)
    }

    @Test
    fun `externalUrl が空文字なら拒否する`() {
        assertFailsWith<IllegalArgumentException> {
            EvacuationShelter.create(
                id = EvacuationShelter.Id.of("kobe-city-hall"),
                coordinates = ShelterCoordinates.of(34.6826, 135.1863),
                type = ShelterType.DESIGNATED_EVACUATION_SHELTER,
                facilityCategory = ShelterFacilityCategory.GOVERNMENT,
                media = ShelterMedia("https://example.com/kobe-city-hall.webp"),
                accessible = true,
                localizations = localizationsOf(Language.EN),
                externalUrl = "   ",
            )
        }
    }
}

class ShelterExternalUrlTest {
    @Test
    fun `https URL を生成できる`() {
        val url = ShelterExternalUrl.of("https://www.city.kobe.lg.jp/bosai/")

        assertEquals("https://www.city.kobe.lg.jp/bosai/", url?.value)
    }

    @Test
    fun `前後空白は trim される`() {
        val url = ShelterExternalUrl.of("  https://example.com/evacuation-info  ")

        assertEquals("https://example.com/evacuation-info", url?.value)
    }

    @Test
    fun `null は null を返す`() {
        assertNull(ShelterExternalUrl.of(null))
    }

    @Test
    fun `空文字は拒否する`() {
        assertFailsWith<IllegalArgumentException> {
            ShelterExternalUrl.of("   ")
        }
    }

    @Test
    fun `http 以外の scheme は拒否する`() {
        assertFailsWith<IllegalArgumentException> {
            ShelterExternalUrl.of("javascript:alert(1)")
        }
        assertFailsWith<IllegalArgumentException> {
            ShelterExternalUrl.of("ftp://example.com/")
        }
    }

    @Test
    fun `URI として不正な文字列は拒否する`() {
        assertFailsWith<IllegalArgumentException> {
            ShelterExternalUrl.of("not-a-url")
        }
    }

    @Test
    fun `host が無い URL は拒否する`() {
        assertFailsWith<IllegalArgumentException> {
            ShelterExternalUrl.of("https://")
        }
    }
}

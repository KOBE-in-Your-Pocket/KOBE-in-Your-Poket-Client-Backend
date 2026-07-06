package com.kobeinyourpocket.backend.domain.evacuation

import com.kobeinyourpocket.backend.domain.evacuation.aggregate.EvacuationShelter
import com.kobeinyourpocket.backend.domain.evacuation.vo.ShelterCapacity
import com.kobeinyourpocket.backend.domain.evacuation.vo.ShelterCoordinates
import com.kobeinyourpocket.backend.domain.evacuation.vo.ShelterExternalUrl
import com.kobeinyourpocket.backend.domain.evacuation.vo.ShelterFacilityCategory
import com.kobeinyourpocket.backend.domain.evacuation.vo.ShelterId
import com.kobeinyourpocket.backend.domain.evacuation.vo.ShelterMedia
import com.kobeinyourpocket.backend.domain.evacuation.vo.ShelterType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ShelterIdTest {
    @Test
    fun `slug 形式の ID を生成できる`() {
        assertEquals("kobe-city-hall", ShelterId.of("kobe-city-hall").value)
    }

    @Test
    fun `前後空白は trim される`() {
        assertEquals("kobe-city-hall", ShelterId.of("  kobe-city-hall  ").value)
    }

    @Test
    fun `空文字は拒否する`() {
        assertFailsWith<IllegalArgumentException> {
            ShelterId.of("   ")
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
    fun `Client と同じ wireValue を持つ`() {
        assertEquals("emergency", ShelterType.EMERGENCY.wireValue)
        assertEquals("designated", ShelterType.DESIGNATED.wireValue)
        assertEquals("both", ShelterType.BOTH.wireValue)
    }

    @Test
    fun `wireValue から解決できる`() {
        assertEquals(ShelterType.EMERGENCY, ShelterType.of("emergency"))
        assertEquals(ShelterType.DESIGNATED, ShelterType.of("  DESIGNATED  "))
        assertEquals(ShelterType.BOTH, ShelterType.of("both"))
    }

    @Test
    fun `未対応値は null を返す`() {
        assertNull(ShelterType.of("unknown"))
        assertNull(ShelterType.of(""))
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
    fun `wireValue から解決できる`() {
        assertEquals(ShelterFacilityCategory.GOVERNMENT, ShelterFacilityCategory.of("government"))
        assertEquals(ShelterFacilityCategory.SCHOOL, ShelterFacilityCategory.of("  SCHOOL  "))
    }

    @Test
    fun `未対応値は null を返す`() {
        assertNull(ShelterFacilityCategory.of("hospital"))
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

class EvacuationShelterTest {
    @Test
    fun `言語非依存フィールドで EvacuationShelter を生成できる`() {
        val shelter =
            EvacuationShelter.create(
                id = ShelterId.of("kobe-city-hall"),
                coordinates = ShelterCoordinates.of(34.6826, 135.1863),
                type = ShelterType.BOTH,
                facilityCategory = ShelterFacilityCategory.GOVERNMENT,
                media = ShelterMedia("https://example.com/kobe-city-hall.webp"),
                accessible = true,
            )

        assertEquals("kobe-city-hall", shelter.id.value)
        assertEquals(ShelterType.BOTH, shelter.type)
        assertNull(shelter.capacity)
        assertNull(shelter.externalUrl)
    }

    @Test
    fun `capacity と externalUrl は任意`() {
        val shelter =
            EvacuationShelter.create(
                id = ShelterId.of("nunobiki-park"),
                coordinates = ShelterCoordinates.of(34.7050, 135.1900),
                type = ShelterType.EMERGENCY,
                facilityCategory = ShelterFacilityCategory.PARK,
                media = ShelterMedia("https://example.com/nunobiki-park.webp"),
                accessible = false,
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
                id = ShelterId.of("kobe-city-hall"),
                coordinates = ShelterCoordinates.of(34.6826, 135.1863),
                type = ShelterType.DESIGNATED,
                facilityCategory = ShelterFacilityCategory.GOVERNMENT,
                media = ShelterMedia("https://example.com/kobe-city-hall.webp"),
                accessible = true,
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

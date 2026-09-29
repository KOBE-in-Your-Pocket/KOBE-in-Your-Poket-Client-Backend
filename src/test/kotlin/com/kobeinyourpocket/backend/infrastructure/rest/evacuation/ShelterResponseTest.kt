package com.kobeinyourpocket.backend.infrastructure.rest.evacuation

import com.kobeinyourpocket.backend.application.evacuation.query.ShelterView
import tools.jackson.databind.json.JsonMapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShelterResponseTest {
    private val objectMapper = JsonMapper.builder().build()

    private val view =
        ShelterView(
            id = "kobe-001",
            name = "東灘小学校",
            address = "神戸市東灘区深江北町2-4-1",
            latitude = 34.7248161,
            longitude = 135.2944292,
            type = "both",
            siting = "indoor",
            suitabilityLandslide = "suitable",
            suitabilityFlood = "suitable",
            suitabilityTsunami = "suitable",
            suitabilityLargeFire = "not-applicable",
            petAcceptance = "accepted",
            phoneNumber = "078-411-0556",
            note = null,
        )

    @Test
    fun `ShelterView の全フィールドを Client EvacuationShelter 形に引き継ぐ`() {
        val response = ShelterResponse.from(view)

        assertEquals(view.id, response.id)
        assertEquals(view.name, response.name)
        assertEquals(view.address, response.address)
        assertEquals(view.latitude, response.coordinates.latitude)
        assertEquals(view.longitude, response.coordinates.longitude)
        assertEquals(view.type, response.type)
        assertEquals(view.siting, response.siting)
        assertEquals(view.petAcceptance, response.petAcceptance)
        assertEquals(view.phoneNumber, response.phoneNumber)
        assertEquals(view.note, response.note)
    }

    @Test
    fun `suitability は災害種別 slug をキーに全種別を含む`() {
        // Client は絞り込みでこのキーを引く。欠けると「対応していない」と
        // 「情報が無い」の区別が付かなくなる（#180）。
        val response = ShelterResponse.from(view)

        assertEquals(
            mapOf(
                "landslide" to "suitable",
                "flood" to "suitable",
                "tsunami" to "suitable",
                "large-fire" to "not-applicable",
            ),
            response.suitability,
        )
    }

    @Test
    fun `対象外の災害種別もキーごと落とさない`() {
        val response = ShelterResponse.from(view.copy(suitabilityLargeFire = "not-applicable"))

        val node = objectMapper.readTree(objectMapper.writeValueAsString(response))

        assertTrue(node.get("suitability").has("large-fire"))
        assertEquals("not-applicable", node.get("suitability").get("large-fire").asString())
    }

    @Test
    fun `phoneNumber・note が null の ShelterView は JSON から除外される`() {
        val response = ShelterResponse.from(view.copy(phoneNumber = null, note = null))

        val json = objectMapper.writeValueAsString(response)
        val node = objectMapper.readTree(json)

        assertFalse(node.has("phoneNumber"))
        assertFalse(node.has("note"))
    }
}

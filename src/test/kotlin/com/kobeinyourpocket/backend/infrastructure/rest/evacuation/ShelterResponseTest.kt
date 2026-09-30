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
    // ---- 公開済み 1.0.0 のための互換（Client #557）----
    //
    // 1.0.0 はレスポンスを検証せずそのまま SQLite へ流すため、これらが欠けると
    // 新規インストールの利用者に避難所が 1 件も出なくなる。

    @Test
    fun `1_0_0 が必須で読むキーを含む`() {
        val response = ShelterResponse.from(view)

        assertEquals("government", response.facilityCategory)
        assertEquals("", response.media.imageUrl)
        assertEquals(false, response.accessible)
    }

    @Test
    fun `互換キーは JSON からも落とさない`() {
        val node = objectMapper.readTree(objectMapper.writeValueAsString(ShelterResponse.from(view)))

        assertTrue(node.has("facilityCategory"))
        assertTrue(node.has("media"))
        assertTrue(node.get("media").has("imageUrl"))
        assertTrue(node.has("accessible"))
    }

    @Test
    fun `屋外は公園、屋内は公共施設に当てる`() {
        // 元データは施設種別を持たない。屋内を school にすると大学・会館・体育館で嘘になるため、
        // どの避難所でも事実に反しない government を使う。
        assertEquals("park", ShelterResponse.from(view.copy(siting = "outdoor")).facilityCategory)
        assertEquals("government", ShelterResponse.from(view.copy(siting = "indoor")).facilityCategory)
    }
}

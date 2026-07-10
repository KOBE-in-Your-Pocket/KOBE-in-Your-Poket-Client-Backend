package com.kobeinyourpocket.backend.infrastructure.rest.evacuation

import com.kobeinyourpocket.backend.application.evacuation.query.ShelterView
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import tools.jackson.databind.json.JsonMapper

class ShelterResponseTest {
    private val objectMapper = JsonMapper.builder().build()

    private val view =
        ShelterView(
            id = "kobe-city-hall",
            name = "神戸市役所",
            address = "兵庫県神戸市中央区加納町6丁目5-1",
            latitude = 34.6826,
            longitude = 135.1863,
            type = "dual-use",
            facilityCategory = "government",
            imageUrl = "https://example.com/kobe-city-hall.webp",
            capacity = 500,
            accessible = true,
            externalUrl = "https://example.com/kobe-city-hall",
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
        assertEquals(view.facilityCategory, response.facilityCategory)
        assertEquals(view.imageUrl, response.media.imageUrl)
        assertEquals(view.capacity, response.capacity)
        assertEquals(view.accessible, response.accessible)
        assertEquals(view.externalUrl, response.externalUrl)
    }

    @Test
    fun `capacity・externalUrl が null の ShelterView は JSON から除外される`() {
        val response = ShelterResponse.from(view.copy(capacity = null, externalUrl = null))

        val json = objectMapper.writeValueAsString(response)
        val node = objectMapper.readTree(json)

        assertFalse(node.has("capacity"))
        assertFalse(node.has("externalUrl"))
    }
}

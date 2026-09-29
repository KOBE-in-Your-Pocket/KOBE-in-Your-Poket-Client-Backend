package com.kobeinyourpocket.backend.infrastructure.rest.evacuation

import com.kobeinyourpocket.backend.application.evacuation.query.ShelterDatasetMetadataView
import com.kobeinyourpocket.backend.application.evacuation.query.ShelterView
import tools.jackson.databind.json.JsonMapper
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class ShelterListResponseTest {
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

    private val metadata =
        ShelterDatasetMetadataView(
            source = "神戸市オープンデータ「指定緊急避難場所・指定避難所」(CC BY 4.0)",
            asOf = LocalDate.of(2025, 4, 2),
            updatedAt = Instant.parse("2025-04-02T00:00:00Z"),
        )

    @Test
    fun `data に ShelterView 一覧・meta にデータセット情報を格納する`() {
        val response = ShelterListResponse.of(listOf(view), metadata)

        assertEquals("kobe-001", response.data.single().id)
        assertEquals(metadata.source, response.meta.source)
        assertEquals(metadata.asOf, response.meta.asOf)
        assertEquals(metadata.updatedAt, response.meta.updatedAt)
    }

    @Test
    fun `JSON は data 配列と meta オブジェクトのトップレベル封筒になる`() {
        val response = ShelterListResponse.of(listOf(view), metadata)

        val json = objectMapper.writeValueAsString(response)
        val node = objectMapper.readTree(json)

        assertEquals(1, node["data"].size())
        assertEquals("kobe-001", node["data"][0]["id"].asString())
        assertEquals(metadata.source, node["meta"]["source"].asString())
        assertEquals("2025-04-02", node["meta"]["asOf"].asString())
        assertEquals("2025-04-02T00:00:00Z", node["meta"]["updatedAt"].asString())
    }
}

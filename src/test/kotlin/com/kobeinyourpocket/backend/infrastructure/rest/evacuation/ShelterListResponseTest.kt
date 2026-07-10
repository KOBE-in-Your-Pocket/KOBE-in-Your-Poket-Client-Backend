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

    private val metadata =
        ShelterDatasetMetadataView(
            source = "神戸市オープンデータポータル「神戸市避難場所」(CC BY 2.1 JP)",
            asOf = LocalDate.of(2025, 4, 2),
            updatedAt = Instant.parse("2025-04-02T00:00:00Z"),
        )

    @Test
    fun `data に ShelterView 一覧・meta にデータセット情報を格納する`() {
        val response = ShelterListResponse.of(listOf(view), metadata)

        assertEquals("kobe-city-hall", response.data.single().id)
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
        assertEquals("kobe-city-hall", node["data"][0]["id"].asString())
        assertEquals(metadata.source, node["meta"]["source"].asString())
        assertEquals("2025-04-02", node["meta"]["asOf"].asString())
        assertEquals("2025-04-02T00:00:00Z", node["meta"]["updatedAt"].asString())
    }
}

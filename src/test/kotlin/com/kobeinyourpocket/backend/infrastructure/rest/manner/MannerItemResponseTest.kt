package com.kobeinyourpocket.backend.infrastructure.rest.manner

import com.kobeinyourpocket.backend.application.manner.query.MannerItemView
import com.kobeinyourpocket.backend.application.manner.query.MannerLocalizationView
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * `MannerItemResponse.from` のフィールド保持テスト（#73）。
 *
 * kind / scope / spotId 絞り込みはサーバー側では実装せず Client 側フィルタで成立させる方針（M-1）。
 * その前提が破綻しないよう、レスポンスが絞り込みに必要な kind / scope / relatedSpotIds を
 * 欠落・改変なく [MannerItemView] から引き継ぐことを固定する。
 */
class MannerItemResponseTest {
    private val view =
        MannerItemView(
            id = "arima-onsen-bathing",
            title = "有馬温泉の入浴マナー",
            description = "湯船に入る前にかけ湯で体を流しましょう。",
            icon = "hot-spring",
            iconUrl = null,
            kind = "manner",
            scope = "local",
            relatedSpotIds = listOf("arima-onsen", "kobe-port-tower"),
            localizations =
                mapOf(
                    "ja" to MannerLocalizationView("有馬温泉の入浴マナー", "湯船に入る前にかけ湯で体を流しましょう。"),
                    "en" to MannerLocalizationView("Arima Onsen bathing etiquette", "Rinse your body before entering the bath."),
                ),
        )

    @Test
    fun `Client 側フィルタ用の kind scope relatedSpotIds を欠落なく引き継ぐ`() {
        val response = MannerItemResponse.from(view)

        assertEquals("manner", response.kind)
        assertEquals("local", response.scope)
        assertEquals(listOf("arima-onsen", "kobe-port-tower"), response.relatedSpotIds)
    }

    @Test
    fun `id title description icon も含めた全フィールドを一致させる`() {
        val response = MannerItemResponse.from(view)

        assertEquals(view.id, response.id)
        assertEquals(view.title, response.title)
        assertEquals(view.description, response.description)
        assertEquals(view.icon, response.icon)
    }

    @Test
    fun `全言語の localizations をそのまま引き継ぐ`() {
        val response = MannerItemResponse.from(view)

        assertEquals(setOf("ja", "en"), response.localizations.keys)
        assertEquals("Arima Onsen bathing etiquette", response.localizations.getValue("en").title)
    }

    @Test
    fun `画像 URL を持つ項目は iconUrl を引き継ぐ`() {
        val response = MannerItemResponse.from(view.copy(icon = null, iconUrl = "https://example.com/a.png"))

        assertEquals(null, response.icon)
        assertEquals("https://example.com/a.png", response.iconUrl)
    }

    @Test
    fun `relatedSpotIds が空でも空リストとして保持する`() {
        val response = MannerItemResponse.from(view.copy(relatedSpotIds = emptyList()))

        assertEquals(emptyList(), response.relatedSpotIds)
    }
}

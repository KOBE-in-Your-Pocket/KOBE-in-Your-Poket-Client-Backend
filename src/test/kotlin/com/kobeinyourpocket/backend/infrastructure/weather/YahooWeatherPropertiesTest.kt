package com.kobeinyourpocket.backend.infrastructure.weather

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class YahooWeatherPropertiesTest {
    @Test
    fun `Client ID が空なら未設定として扱う`() {
        assertFalse(YahooWeatherProperties().isConfigured)
        assertFalse(YahooWeatherProperties(clientId = "  ").isConfigured)
    }

    @Test
    fun `Client ID があれば設定済みとして扱う`() {
        assertTrue(YahooWeatherProperties(clientId = "dummy-client-id").isConfigured)
    }

    @Test
    fun `toString に Client ID を含めない`() {
        val text = YahooWeatherProperties(clientId = "dummy-client-id").toString()

        assertFalse(text.contains("dummy-client-id"))
    }
}

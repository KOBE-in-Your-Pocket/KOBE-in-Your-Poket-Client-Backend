package com.kobeinyourpocket.backend.infrastructure.supabase

import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** GoTrue レスポンスの JSON マッピングと表示名フォールバック（#89-c）。 */
class SupabaseAuthClientTest {
    private val mapper = jacksonObjectMapper()

    @Test
    fun `id_token グラント応答から email と user_metadata をパースする`() {
        val json =
            """
            {
              "access_token": "at",
              "refresh_token": "rt",
              "expires_in": 3600,
              "token_type": "bearer",
              "user": {
                "id": "11111111-1111-1111-1111-111111111111",
                "email": "taro@example.com",
                "user_metadata": {
                  "full_name": "Google Taro",
                  "name": "Taro",
                  "avatar_url": "https://example.com/a.png"
                },
                "role": "authenticated"
              },
              "weak_password": null
            }
            """.trimIndent()

        val response = mapper.readValue<SupabaseAuthClient.GoTrueSessionResponse>(json)

        assertEquals("at", response.accessToken)
        assertEquals("rt", response.refreshToken)
        assertEquals(3600, response.expiresIn)
        assertEquals("11111111-1111-1111-1111-111111111111", response.user?.id)
        assertEquals("taro@example.com", response.user?.email)
        assertEquals("Google Taro", response.user?.userMetadata?.displayName())
    }

    @Test
    fun `displayName は full_name を優先する`() {
        val metadata = SupabaseAuthClient.GoTrueUserMetadata(fullName = "Full", name = "Short")

        assertEquals("Full", metadata.displayName())
    }

    @Test
    fun `full_name が blank なら name にフォールバックする`() {
        val metadata = SupabaseAuthClient.GoTrueUserMetadata(fullName = "  ", name = "Short")

        assertEquals("Short", metadata.displayName())
    }

    @Test
    fun `full_name が無ければ name を使う`() {
        val metadata = SupabaseAuthClient.GoTrueUserMetadata(fullName = null, name = "Short")

        assertEquals("Short", metadata.displayName())
    }

    @Test
    fun `両方 blank なら null`() {
        val metadata = SupabaseAuthClient.GoTrueUserMetadata(fullName = " ", name = "")

        assertNull(metadata.displayName())
    }

    @Test
    fun `user_metadata 自体が無い応答もパースできる`() {
        val json = """{"access_token":"at","user":{"id":"x"}}"""

        val response = mapper.readValue<SupabaseAuthClient.GoTrueSessionResponse>(json)

        assertEquals("x", response.user?.id)
        assertNull(response.user?.email)
        assertNull(response.user?.userMetadata)
    }
}

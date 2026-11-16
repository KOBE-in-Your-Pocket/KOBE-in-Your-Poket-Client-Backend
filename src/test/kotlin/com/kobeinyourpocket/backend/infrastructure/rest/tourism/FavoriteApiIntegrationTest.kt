package com.kobeinyourpocket.backend.infrastructure.rest.tourism

import com.jayway.jsonpath.JsonPath
import com.kobeinyourpocket.backend.domain.user.vo.Role
import com.kobeinyourpocket.backend.infrastructure.persistence.tourism.FavoriteJpaRepository
import com.kobeinyourpocket.backend.infrastructure.persistence.tourism.FavoriteKey
import com.kobeinyourpocket.backend.infrastructure.security.withRole
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * お気に入りの追加・解除 API の統合テスト（#236）。
 * controller → application → JPA → DB を実 Bean で通す。
 *
 * テスト環境は H2（application-test.yml、Hibernate create-drop / Flyway 無効）。
 * H2 には spot への外部キーが無いため、存在確認のあとにスポットが削除された場合の 404 は
 * [com.kobeinyourpocket.backend.application.tourism.command.AddFavoriteServiceTest] で確かめる。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FavoriteApiIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var favoriteJpa: FavoriteJpaRepository

    private val spotBody =
        """
        {
          "genre": "landmark",
          "coordinates": { "latitude": 34.6826, "longitude": 135.1863 },
          "imageUrl": "https://example.com/kobe-port-tower.webp",
          "localizations": {
            "ja": { "name": "神戸ポートタワー", "categoryLabel": "ランドマーク", "description": "神戸のシンボル。",
                    "businessHours": "9:00-23:00", "address": "神戸市中央区波止場町5-5" },
            "en": { "name": "Kobe Port Tower", "categoryLabel": "Landmark", "description": "The symbol of Kobe.",
                    "businessHours": "9:00-23:00", "address": "5-5 Hatobacho, Chuo-ku, Kobe" },
            "zh": { "name": "神户港塔", "categoryLabel": "地标", "description": "神户的象征。",
                    "businessHours": "9:00-23:00", "address": "神户市中央区波止场町5-5" },
            "ko": { "name": "고베 포트 타워", "categoryLabel": "랜드마크", "description": "고베의 상징.",
                    "businessHours": "9:00-23:00", "address": "고베시 주오구 하토바초 5-5" }
          }
        }
        """.trimIndent()

    private fun registerSpot(): String {
        val result =
            mockMvc
                .perform(
                    post("/api/v1/tourism/spots")
                        .with(withRole(Role.OPERATOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(spotBody),
                ).andExpect(status().isCreated)
                .andReturn()
        return JsonPath.read(result.response.contentAsString, "$.id")
    }

    private fun add(
        spotId: String,
        subject: String? = VIEWER,
    ): ResultActions {
        val request = put("/api/v1/users/me/favorites/$spotId")
        if (subject != null) request.with(withRole(Role.GENERAL, subject))
        return mockMvc.perform(request)
    }

    private fun remove(
        spotId: String,
        subject: String? = VIEWER,
    ): ResultActions {
        val request = delete("/api/v1/users/me/favorites/$spotId")
        if (subject != null) request.with(withRole(Role.GENERAL, subject))
        return mockMvc.perform(request)
    }

    private fun isFavorite(
        subject: String,
        spotId: String,
    ): Boolean = favoriteJpa.existsById(FavoriteKey(userId = UUID.fromString(subject), spotId = spotId))

    // ---- 追加 ----

    @Test
    fun `追加すると 204 で本人のお気に入りとして保存される`() {
        val spotId = registerSpot()

        add(spotId).andExpect(status().isNoContent)

        assertEquals(true, isFavorite(VIEWER, spotId))
        assertEquals(1, favoriteJpa.count())
    }

    @Test
    fun `同じスポットを 2 回追加しても 204 で 1 件のまま`() {
        val spotId = registerSpot()

        add(spotId).andExpect(status().isNoContent)
        add(spotId).andExpect(status().isNoContent)

        assertEquals(1, favoriteJpa.count())
    }

    @Test
    fun `存在しないスポットを追加すると 404 で保存しない`() {
        add("no-such-spot").andExpect(status().isNotFound)

        assertEquals(0, favoriteJpa.count())
    }

    // ---- 解除 ----

    @Test
    fun `解除すると 204 で本人の分だけ消えてほかの人の分は残る`() {
        val spotId = registerSpot()
        add(spotId, subject = VIEWER).andExpect(status().isNoContent)
        add(spotId, subject = FRIEND).andExpect(status().isNoContent)

        remove(spotId, subject = VIEWER).andExpect(status().isNoContent)

        assertEquals(false, isFavorite(VIEWER, spotId))
        assertEquals(true, isFavorite(FRIEND, spotId))
    }

    @Test
    fun `登録していないスポットの解除も 204`() {
        remove("no-such-spot").andExpect(status().isNoContent)
    }

    // ---- 認証 ----

    @Test
    fun `未ログインは追加も解除も 401`() {
        val spotId = registerSpot()

        add(spotId, subject = null).andExpect(status().isUnauthorized)
        remove(spotId, subject = null).andExpect(status().isUnauthorized)

        assertEquals(0, favoriteJpa.count())
    }

    private companion object {
        const val VIEWER = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
        const val FRIEND = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"
    }
}

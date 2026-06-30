package com.kobeinyourpocket.backend.infrastructure.rest.tourism

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import kotlin.test.Test

/**
 * 一覧/登録の統合テスト（#27）。controller → application → JPA → DB を実 Bean で通し、
 * `spot` / `spot_localization` への永続化とモック互換レスポンスを end-to-end で検証する。
 *
 * テスト環境は H2（`application-test.yml`、Hibernate create-drop / Flyway 無効）。
 * 各テストは [Transactional] でロールバックし相互に独立させる（§5.1 / #27）。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SpotApiIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    private val registerBody =
        """
        {
          "genre": "landmark",
          "coordinates": { "latitude": 34.6826, "longitude": 135.1863 },
          "imageUrl": "https://example.com/kobe-port-tower.webp",
          "localizations": {
            "ja": {
              "name": "神戸ポートタワー",
              "categoryLabel": "ランドマーク",
              "description": "神戸のシンボル。",
              "businessHours": "9:00-23:00"
            },
            "en": {
              "name": "Kobe Port Tower",
              "categoryLabel": "Landmark",
              "description": "The symbol of Kobe.",
              "businessHours": "9:00-23:00"
            }
          }
        }
        """.trimIndent()

    private fun registerPortTower() =
        mockMvc
            .perform(
                post("/api/v1/tourism/spots")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(registerBody),
            )

    @Test
    fun `POST で登録し 201 とモック互換 Spot JSON を返す`() {
        registerPortTower()
            .andExpect(status().isCreated)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.name").value("神戸ポートタワー"))
            .andExpect(jsonPath("$.genre").value("landmark"))
            .andExpect(jsonPath("$.description").value("神戸のシンボル。"))
            .andExpect(jsonPath("$.coordinates.latitude").value(34.6826))
            .andExpect(jsonPath("$.coordinates.longitude").value(135.1863))
            .andExpect(jsonPath("$.businessHours").value("9:00-23:00"))
            .andExpect(jsonPath("$.category.label").value("ランドマーク"))
            .andExpect(jsonPath("$.media.imageUrl").value("https://example.com/kobe-port-tower.webp"))
            // feature ① では rating は常に null（NON_NULL でキー自体が出ない）
            .andExpect(jsonPath("$.rating").doesNotExist())
    }

    @Test
    fun `登録したスポットを GET lang=ja で取得できる`() {
        registerPortTower().andExpect(status().isCreated)

        mockMvc
            .perform(get("/api/v1/tourism/spots?lang=ja"))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].name").value("神戸ポートタワー"))
            .andExpect(jsonPath("$[0].category.label").value("ランドマーク"))
            .andExpect(jsonPath("$[0].coordinates.latitude").value(34.6826))
            .andExpect(jsonPath("$[0].media.imageUrl").value("https://example.com/kobe-port-tower.webp"))
            .andExpect(jsonPath("$[0].rating").doesNotExist())
    }

    @Test
    fun `GET lang=en で英語ローカライズを返す`() {
        registerPortTower().andExpect(status().isCreated)

        mockMvc
            .perform(get("/api/v1/tourism/spots?lang=en"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].name").value("Kobe Port Tower"))
            .andExpect(jsonPath("$[0].category.label").value("Landmark"))
            .andExpect(jsonPath("$[0].description").value("The symbol of Kobe."))
    }

    @Test
    fun `未収録言語は ja へフォールバックする`() {
        registerPortTower().andExpect(status().isCreated)

        // ko は localizations 未収録 → ja 解決
        mockMvc
            .perform(get("/api/v1/tourism/spots?lang=ko"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].name").value("神戸ポートタワー"))
            .andExpect(jsonPath("$[0].category.label").value("ランドマーク"))
    }

    @Test
    fun `lang 未指定でも ja で取得できる`() {
        registerPortTower().andExpect(status().isCreated)

        mockMvc
            .perform(get("/api/v1/tourism/spots"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].name").value("神戸ポートタワー"))
    }
}

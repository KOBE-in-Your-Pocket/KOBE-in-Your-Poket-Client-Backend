package com.kobeinyourpocket.backend.infrastructure.rest.stats

import com.kobeinyourpocket.backend.domain.user.vo.Role
import com.kobeinyourpocket.backend.infrastructure.persistence.tourism.SpotEntity
import com.kobeinyourpocket.backend.infrastructure.persistence.tourism.SpotJpaRepository
import com.kobeinyourpocket.backend.infrastructure.persistence.tourism.SpotLocalizationEntity
import com.kobeinyourpocket.backend.infrastructure.persistence.tourism.SpotLocalizationId
import com.kobeinyourpocket.backend.infrastructure.persistence.tourism.SpotLocalizationJpaRepository
import com.kobeinyourpocket.backend.infrastructure.security.withRole
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import kotlin.test.Test

/**
 * 運営向けのスポット別閲覧数 API の統合テスト（#241）。
 * 閲覧は公開 API（`POST /api/v1/tourism/spots/{spotId}/views`）で記録し、controller → application → JPA → DB を実 Bean で通す。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SpotViewStatsApiIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var spotJpa: SpotJpaRepository

    @Autowired
    private lateinit var localizationJpa: SpotLocalizationJpaRepository

    private fun saveSpot(
        id: String,
        jaName: String,
    ) {
        spotJpa.saveAndFlush(
            SpotEntity(id = id, genre = "landmark", latitude = 34.6, longitude = 135.1, imageUrl = "https://example.com/$id.jpg"),
        )
        localizationJpa.saveAndFlush(
            SpotLocalizationEntity(
                id = SpotLocalizationId(spotId = id, language = "ja"),
                name = jaName,
                categoryLabel = "",
                description = "",
                businessHours = "",
                address = "",
            ),
        )
    }

    private fun view(
        spotId: String,
        times: Int,
    ) {
        repeat(times) {
            mockMvc.perform(post("/api/v1/tourism/spots/$spotId/views")).andExpect(status().isNoContent)
        }
    }

    @Test
    fun `OPERATOR は閲覧数の多い順にスポット名つきで取れる`() {
        saveSpot("kobe-port-tower", "神戸ポートタワー")
        saveSpot("arima-onsen", "有馬温泉")
        saveSpot("kitano-ijinkan", "北野異人館")
        view("kobe-port-tower", 1)
        view("arima-onsen", 3)

        mockMvc
            .perform(get("/api/v1/stats/spot-views").param("lang", "ja").with(withRole(Role.OPERATOR)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].spotId").value("arima-onsen"))
            .andExpect(jsonPath("$[0].name").value("有馬温泉"))
            .andExpect(jsonPath("$[0].viewCount").value(3))
            .andExpect(jsonPath("$[1].spotId").value("kobe-port-tower"))
            .andExpect(jsonPath("$[1].name").value("神戸ポートタワー"))
            .andExpect(jsonPath("$[1].viewCount").value(1))
    }

    @Test
    fun `ADMIN もロール階層で取れる`() {
        mockMvc
            .perform(get("/api/v1/stats/spot-views").with(withRole(Role.ADMIN)))
            .andExpect(status().isOk)
    }

    @Test
    fun `一般ユーザーは 403`() {
        mockMvc
            .perform(get("/api/v1/stats/spot-views").with(withRole(Role.GENERAL)))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `未認証は 401`() {
        mockMvc.perform(get("/api/v1/stats/spot-views")).andExpect(status().isUnauthorized)
    }
}

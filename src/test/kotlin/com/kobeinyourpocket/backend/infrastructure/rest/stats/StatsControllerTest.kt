package com.kobeinyourpocket.backend.infrastructure.rest.stats

import com.kobeinyourpocket.backend.application.stats.query.DashboardStatsView
import com.kobeinyourpocket.backend.application.stats.query.EntityCountsView
import com.kobeinyourpocket.backend.application.stats.query.GetDashboardStatsService
import com.kobeinyourpocket.backend.application.stats.query.PopularSpotView
import com.kobeinyourpocket.backend.application.stats.query.RecentReviewView
import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.infrastructure.rest.common.GlobalExceptionHandler
import org.mockito.BDDMockito.given
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
import kotlin.test.Test

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(StatsController::class)
@Import(GlobalExceptionHandler::class)
class StatsControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var getDashboardStatsService: GetDashboardStatsService

    private val stats =
        DashboardStatsView(
            totals = EntityCountsView(users = 128, spots = 15, reviews = 42),
            thisMonth = EntityCountsView(users = 12, spots = 2, reviews = 7),
            lastMonth = EntityCountsView(users = 9, spots = 0, reviews = 5),
            popularSpots =
                listOf(
                    PopularSpotView(spotId = "arima-onsen", name = "有馬温泉", reviewCount = 2),
                    PopularSpotView(spotId = "kobe-port-tower", name = "神戸ポートタワー", reviewCount = 1),
                ),
            recentReviews =
                listOf(
                    RecentReviewView(
                        id = "8c7d0b63-ce99-435f-a360-ece036654fb4",
                        spotId = "arima-onsen",
                        spotName = "有馬温泉",
                        authorName = "田中 美咲",
                        rating = 5,
                        postedAt = Instant.parse("2026-08-17T06:07:54Z"),
                        language = "ja",
                    ),
                ),
        )

    @Test
    fun `総数と前月比の元データを返す`() {
        given(getDashboardStatsService.getStats(Language.JA)).willReturn(stats)

        mockMvc
            .perform(get("/api/v1/stats").param("lang", "ja"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totals.users").value(128))
            .andExpect(jsonPath("$.totals.spots").value(15))
            .andExpect(jsonPath("$.totals.reviews").value(42))
            .andExpect(jsonPath("$.newUsers.thisMonth").value(12))
            .andExpect(jsonPath("$.newUsers.lastMonth").value(9))
            .andExpect(jsonPath("$.newSpots.thisMonth").value(2))
            .andExpect(jsonPath("$.newReviews.lastMonth").value(5))
    }

    @Test
    fun `人気スポットと直近レビューを返す`() {
        given(getDashboardStatsService.getStats(Language.JA)).willReturn(stats)

        mockMvc
            .perform(get("/api/v1/stats").param("lang", "ja"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.popularSpots[0].spotId").value("arima-onsen"))
            .andExpect(jsonPath("$.popularSpots[0].name").value("有馬温泉"))
            .andExpect(jsonPath("$.popularSpots[0].reviewCount").value(2))
            .andExpect(jsonPath("$.recentReviews[0].spotName").value("有馬温泉"))
            .andExpect(jsonPath("$.recentReviews[0].authorName").value("田中 美咲"))
            .andExpect(jsonPath("$.recentReviews[0].rating.value").value(5))
            .andExpect(jsonPath("$.recentReviews[0].language").value("ja"))
    }

    @Test
    fun `lang 未指定なら Accept-Language を使う`() {
        given(getDashboardStatsService.getStats(Language.EN)).willReturn(stats)

        mockMvc
            .perform(get("/api/v1/stats").header("Accept-Language", "en"))
            .andExpect(status().isOk)

        verify(getDashboardStatsService).getStats(Language.EN)
    }

    @Test
    fun `lang も Accept-Language も無ければ既定言語で解決する`() {
        given(getDashboardStatsService.getStats(Language.DEFAULT)).willReturn(stats)

        mockMvc
            .perform(get("/api/v1/stats"))
            .andExpect(status().isOk)

        verify(getDashboardStatsService).getStats(Language.DEFAULT)
    }
}

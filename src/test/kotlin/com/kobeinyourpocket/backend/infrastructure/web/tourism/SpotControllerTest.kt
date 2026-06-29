package com.kobeinyourpocket.backend.infrastructure.web.tourism

import com.kobeinyourpocket.backend.application.tourism.LocalizedSpot
import com.kobeinyourpocket.backend.application.tourism.SpotService
import com.kobeinyourpocket.backend.domain.tourism.aggregate.Spot
import com.kobeinyourpocket.backend.domain.tourism.vo.Coordinates
import com.kobeinyourpocket.backend.domain.tourism.vo.Genre
import com.kobeinyourpocket.backend.domain.tourism.vo.Language
import com.kobeinyourpocket.backend.domain.tourism.vo.SpotId
import com.kobeinyourpocket.backend.domain.tourism.vo.SpotLocalization
import com.kobeinyourpocket.backend.domain.tourism.vo.SpotMedia
import com.kobeinyourpocket.backend.domain.tourism.vo.SpotRating
import org.mockito.BDDMockito.given
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.Test

@WebMvcTest(SpotController::class)
class SpotControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var spotService: SpotService

    private val portTower =
        LocalizedSpot(
            spot =
                Spot(
                    id = SpotId.of("kobe-port-tower"),
                    genre = Genre.LANDMARK,
                    coordinates = Coordinates.of(34.6826, 135.1863),
                    media = SpotMedia("https://example.com/kobe-port-tower.webp"),
                    rating = SpotRating(4.5),
                ),
            localization = SpotLocalization("神戸ポートタワー", "ランドマーク", "神戸のシンボル。", "9:00-23:00"),
        )

    private val noRating =
        portTower.copy(
            spot = portTower.spot.copy(id = SpotId.of("no-rating"), rating = null),
        )

    @Test
    fun `lang=ja でモック互換 JSON を返す`() {
        given(spotService.listSpots(Language.JA)).willReturn(listOf(portTower, noRating))

        mockMvc
            .perform(get("/api/v1/tourism/spots?lang=ja"))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith("application/json"))
            .andExpect(jsonPath("$[0].id").value("kobe-port-tower"))
            .andExpect(jsonPath("$[0].name").value("神戸ポートタワー"))
            .andExpect(jsonPath("$[0].genre").value("landmark"))
            .andExpect(jsonPath("$[0].description").value("神戸のシンボル。"))
            .andExpect(jsonPath("$[0].coordinates.latitude").value(34.6826))
            .andExpect(jsonPath("$[0].coordinates.longitude").value(135.1863))
            .andExpect(jsonPath("$[0].businessHours").value("9:00-23:00"))
            .andExpect(jsonPath("$[0].category.label").value("ランドマーク"))
            .andExpect(jsonPath("$[0].media.imageUrl").value("https://example.com/kobe-port-tower.webp"))
            .andExpect(jsonPath("$[0].rating.value").value(4.5))
            // rating なしは欠落（フロントの optional rating? と整合）
            .andExpect(jsonPath("$[1].id").value("no-rating"))
            .andExpect(jsonPath("$[1].rating").doesNotExist())
    }

    @Test
    fun `lang クエリを主として言語解決する`() {
        given(spotService.listSpots(Language.EN)).willReturn(emptyList())

        mockMvc
            .perform(get("/api/v1/tourism/spots?lang=en"))
            .andExpect(status().isOk)

        verify(spotService).listSpots(Language.EN)
    }

    @Test
    fun `lang 未指定なら Accept-Language を従として解決する`() {
        given(spotService.listSpots(Language.KO)).willReturn(emptyList())

        mockMvc
            .perform(get("/api/v1/tourism/spots").header("Accept-Language", "ko-KR,ko;q=0.9,en;q=0.8"))
            .andExpect(status().isOk)

        verify(spotService).listSpots(Language.KO)
    }

    @Test
    fun `lang も Accept-Language も無ければ ja へフォールバックする`() {
        given(spotService.listSpots(Language.JA)).willReturn(emptyList())

        mockMvc
            .perform(get("/api/v1/tourism/spots"))
            .andExpect(status().isOk)

        verify(spotService).listSpots(Language.JA)
    }
}

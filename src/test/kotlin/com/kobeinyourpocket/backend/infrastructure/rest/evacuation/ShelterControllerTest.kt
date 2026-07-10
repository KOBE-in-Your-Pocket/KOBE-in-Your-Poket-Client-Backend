package com.kobeinyourpocket.backend.infrastructure.rest.evacuation

import com.kobeinyourpocket.backend.application.evacuation.query.ListSheltersService
import com.kobeinyourpocket.backend.application.evacuation.query.ShelterView
import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.infrastructure.rest.common.GlobalExceptionHandler
import org.mockito.BDDMockito.given
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.Test

@WebMvcTest(ShelterController::class)
@Import(GlobalExceptionHandler::class)
class ShelterControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var listSheltersService: ListSheltersService

    private val kobeCityHall =
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

    private val minimalShelter =
        ShelterView(
            id = "minimal-shelter",
            name = "Minimal Park",
            address = "Somewhere",
            latitude = 34.0,
            longitude = 135.0,
            type = "designated-emergency-evacuation-site",
            facilityCategory = "park",
            imageUrl = "https://example.com/minimal.webp",
            capacity = null,
            accessible = false,
            externalUrl = null,
        )

    @Test
    fun `lang=ja で Client EvacuationShelter 形の JSON を返す`() {
        given(listSheltersService.listShelters(Language.JA)).willReturn(listOf(kobeCityHall, minimalShelter))

        mockMvc
            .perform(get("/api/v1/evacuation/shelters?lang=ja"))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith("application/json"))
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value("kobe-city-hall"))
            .andExpect(jsonPath("$[0].name").value("神戸市役所"))
            .andExpect(jsonPath("$[0].address").value("兵庫県神戸市中央区加納町6丁目5-1"))
            .andExpect(jsonPath("$[0].coordinates.latitude").value(34.6826))
            .andExpect(jsonPath("$[0].coordinates.longitude").value(135.1863))
            .andExpect(jsonPath("$[0].type").value("dual-use"))
            .andExpect(jsonPath("$[0].facilityCategory").value("government"))
            .andExpect(jsonPath("$[0].media.imageUrl").value("https://example.com/kobe-city-hall.webp"))
            .andExpect(jsonPath("$[0].capacity").value(500))
            .andExpect(jsonPath("$[0].accessible").value(true))
            .andExpect(jsonPath("$[0].externalUrl").value("https://example.com/kobe-city-hall"))
            .andExpect(jsonPath("$[1].capacity").doesNotExist())
            .andExpect(jsonPath("$[1].externalUrl").doesNotExist())
    }

    @Test
    fun `lang クエリを主として言語解決する`() {
        given(listSheltersService.listShelters(Language.EN)).willReturn(emptyList())

        mockMvc
            .perform(get("/api/v1/evacuation/shelters?lang=en").header("Accept-Language", "ja"))
            .andExpect(status().isOk)

        verify(listSheltersService).listShelters(Language.EN)
    }

    @Test
    fun `lang 未指定なら Accept-Language を従として解決する`() {
        given(listSheltersService.listShelters(Language.KO)).willReturn(emptyList())

        mockMvc
            .perform(get("/api/v1/evacuation/shelters").header("Accept-Language", "ko-KR,ko;q=0.9,en;q=0.8"))
            .andExpect(status().isOk)

        verify(listSheltersService).listShelters(Language.KO)
    }

    @Test
    fun `未対応の言語コードは en へフォールバックする`() {
        given(listSheltersService.listShelters(Language.EN)).willReturn(emptyList())

        mockMvc.perform(get("/api/v1/evacuation/shelters?lang=fr")).andExpect(status().isOk)

        verify(listSheltersService).listShelters(Language.EN)
    }

    @Test
    fun `lang も Accept-Language も無ければ en へフォールバックする`() {
        given(listSheltersService.listShelters(Language.EN)).willReturn(emptyList())

        mockMvc.perform(get("/api/v1/evacuation/shelters")).andExpect(status().isOk)

        verify(listSheltersService).listShelters(Language.EN)
    }
}

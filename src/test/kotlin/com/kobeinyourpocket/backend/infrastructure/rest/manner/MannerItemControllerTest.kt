package com.kobeinyourpocket.backend.infrastructure.rest.manner

import com.kobeinyourpocket.backend.application.manner.query.ListMannerItemsService
import com.kobeinyourpocket.backend.application.manner.query.MannerItemView
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.Test

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(MannerItemController::class)
@Import(GlobalExceptionHandler::class)
class MannerItemControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var listMannerItemsService: ListMannerItemsService

    private val arimaOnsen =
        MannerItemView(
            id = "arima-onsen-bathing",
            title = "有馬温泉の入浴マナー",
            description = "湯船に入る前にかけ湯で体を流しましょう。",
            icon = "hot-spring",
            kind = "manner",
            scope = "local",
            relatedSpotIds = listOf("arima-onsen"),
        )

    private val noLittering =
        MannerItemView(
            id = "no-littering",
            title = "ゴミのポイ捨て禁止",
            description = "ゴミは持ち帰るのが基本です。",
            icon = "trash",
            kind = "rule",
            scope = "japan",
            relatedSpotIds = emptyList(),
        )

    @Test
    fun `lang=ja で Client MannerItem 形の JSON を返す`() {
        given(listMannerItemsService.listMannerItems(Language.JA)).willReturn(listOf(arimaOnsen, noLittering))

        mockMvc
            .perform(get("/api/v1/manner/items?lang=ja"))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith("application/json"))
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value("arima-onsen-bathing"))
            .andExpect(jsonPath("$[0].title").value("有馬温泉の入浴マナー"))
            .andExpect(jsonPath("$[0].description").value("湯船に入る前にかけ湯で体を流しましょう。"))
            .andExpect(jsonPath("$[0].icon").value("hot-spring"))
            .andExpect(jsonPath("$[0].kind").value("manner"))
            .andExpect(jsonPath("$[0].scope").value("local"))
            .andExpect(jsonPath("$[0].relatedSpotIds[0]").value("arima-onsen"))
            .andExpect(jsonPath("$[1].kind").value("rule"))
            .andExpect(jsonPath("$[1].scope").value("japan"))
            .andExpect(jsonPath("$[1].relatedSpotIds.length()").value(0))
    }

    @Test
    fun `lang クエリを主として言語解決する`() {
        given(listMannerItemsService.listMannerItems(Language.EN)).willReturn(emptyList())

        mockMvc
            .perform(get("/api/v1/manner/items?lang=en").header("Accept-Language", "ja"))
            .andExpect(status().isOk)

        verify(listMannerItemsService).listMannerItems(Language.EN)
    }

    @Test
    fun `lang 未指定なら Accept-Language を従として解決する`() {
        given(listMannerItemsService.listMannerItems(Language.KO)).willReturn(emptyList())

        mockMvc
            .perform(get("/api/v1/manner/items").header("Accept-Language", "ko-KR,ko;q=0.9,en;q=0.8"))
            .andExpect(status().isOk)

        verify(listMannerItemsService).listMannerItems(Language.KO)
    }

    @Test
    fun `lang も Accept-Language も無ければ en へフォールバックする`() {
        given(listMannerItemsService.listMannerItems(Language.EN)).willReturn(emptyList())

        mockMvc.perform(get("/api/v1/manner/items")).andExpect(status().isOk)

        verify(listMannerItemsService).listMannerItems(Language.EN)
    }
}

package com.kobeinyourpocket.backend.infrastructure.rest.evacuation

import com.kobeinyourpocket.backend.application.evacuation.command.DeleteShelterService
import com.kobeinyourpocket.backend.application.evacuation.query.GetShelterListService
import com.kobeinyourpocket.backend.application.evacuation.query.ShelterDatasetMetadataView
import com.kobeinyourpocket.backend.application.evacuation.query.ShelterListView
import com.kobeinyourpocket.backend.application.evacuation.query.ShelterView
import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.model.EvacuationShelter
import com.kobeinyourpocket.backend.infrastructure.rest.common.GlobalExceptionHandler
import org.mockito.BDDMockito.given
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(ShelterController::class)
@Import(GlobalExceptionHandler::class)
class ShelterControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var getShelterListService: GetShelterListService

    @MockitoBean
    private lateinit var deleteShelterService: DeleteShelterService

    private val metadata =
        ShelterDatasetMetadataView(
            source = "神戸市オープンデータポータル「神戸市避難場所」(CC BY 2.1 JP)",
            asOf = LocalDate.of(2025, 4, 2),
            updatedAt = Instant.parse("2025-04-02T00:00:00Z"),
        )

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

    private fun stub(
        language: Language,
        shelters: List<ShelterView>,
    ) {
        given(getShelterListService.getShelterList(language)).willReturn(ShelterListView(shelters, metadata))
    }

    @Test
    fun `lang=ja で Client EvacuationShelter 形の JSON を data に返す`() {
        stub(Language.JA, listOf(kobeCityHall, minimalShelter))

        mockMvc
            .perform(get("/api/v1/evacuation/shelters?lang=ja"))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith("application/json"))
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].id").value("kobe-city-hall"))
            .andExpect(jsonPath("$.data[0].name").value("神戸市役所"))
            .andExpect(jsonPath("$.data[0].address").value("兵庫県神戸市中央区加納町6丁目5-1"))
            .andExpect(jsonPath("$.data[0].coordinates.latitude").value(34.6826))
            .andExpect(jsonPath("$.data[0].coordinates.longitude").value(135.1863))
            .andExpect(jsonPath("$.data[0].type").value("dual-use"))
            .andExpect(jsonPath("$.data[0].facilityCategory").value("government"))
            .andExpect(jsonPath("$.data[0].media.imageUrl").value("https://example.com/kobe-city-hall.webp"))
            .andExpect(jsonPath("$.data[0].capacity").value(500))
            .andExpect(jsonPath("$.data[0].accessible").value(true))
            .andExpect(jsonPath("$.data[0].externalUrl").value("https://example.com/kobe-city-hall"))
            .andExpect(jsonPath("$.data[1].capacity").doesNotExist())
            .andExpect(jsonPath("$.data[1].externalUrl").doesNotExist())
    }

    @Test
    fun `meta にデータセットの出典・データ基準日・最終更新日時を返す`() {
        stub(Language.JA, emptyList())

        mockMvc
            .perform(get("/api/v1/evacuation/shelters?lang=ja"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.meta.source").value(metadata.source))
            .andExpect(jsonPath("$.meta.asOf").value("2025-04-02"))
            .andExpect(jsonPath("$.meta.updatedAt").value("2025-04-02T00:00:00Z"))
    }

    @Test
    fun `lang クエリを主として言語解決する`() {
        stub(Language.EN, emptyList())

        mockMvc
            .perform(get("/api/v1/evacuation/shelters?lang=en").header("Accept-Language", "ja"))
            .andExpect(status().isOk)

        verify(getShelterListService).getShelterList(Language.EN)
    }

    @Test
    fun `lang 未指定なら Accept-Language を従として解決する`() {
        stub(Language.KO, emptyList())

        mockMvc
            .perform(get("/api/v1/evacuation/shelters").header("Accept-Language", "ko-KR,ko;q=0.9,en;q=0.8"))
            .andExpect(status().isOk)

        verify(getShelterListService).getShelterList(Language.KO)
    }

    @Test
    fun `未対応の言語コードは en へフォールバックする`() {
        stub(Language.EN, emptyList())

        mockMvc.perform(get("/api/v1/evacuation/shelters?lang=fr")).andExpect(status().isOk)

        verify(getShelterListService).getShelterList(Language.EN)
    }

    @Test
    fun `lang も Accept-Language も無ければ en へフォールバックする`() {
        stub(Language.EN, emptyList())

        mockMvc.perform(get("/api/v1/evacuation/shelters")).andExpect(status().isOk)

        verify(getShelterListService).getShelterList(Language.EN)
    }

    @Test
    fun `DELETE はパスの id を そのまま ユースケースへ渡し 204 を返す`() {
        mockMvc
            .perform(delete("/api/v1/evacuation/shelters/kobe-city-hall"))
            .andExpect(status().isNoContent)

        verify(deleteShelterService).execute(EvacuationShelter.Id.of("kobe-city-hall"))
    }
}

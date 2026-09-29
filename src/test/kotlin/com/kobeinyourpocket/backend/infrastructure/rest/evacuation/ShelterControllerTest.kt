package com.kobeinyourpocket.backend.infrastructure.rest.evacuation

import com.kobeinyourpocket.backend.application.evacuation.query.GetShelterListService
import com.kobeinyourpocket.backend.application.evacuation.query.ShelterDatasetMetadataView
import com.kobeinyourpocket.backend.application.evacuation.query.ShelterListView
import com.kobeinyourpocket.backend.application.evacuation.query.ShelterView
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

    private val metadata =
        ShelterDatasetMetadataView(
            source = "神戸市オープンデータ「指定緊急避難場所・指定避難所」(CC BY 4.0)",
            asOf = LocalDate.of(2025, 4, 2),
            updatedAt = Instant.parse("2025-04-02T00:00:00Z"),
        )

    private val higashinadaElementary =
        ShelterView(
            id = "kobe-001",
            name = "東灘小学校",
            address = "神戸市東灘区深江北町2-4-1",
            latitude = 34.7248161,
            longitude = 135.2944292,
            type = "both",
            siting = "indoor",
            suitabilityLandslide = "suitable",
            suitabilityFlood = "conditional",
            suitabilityTsunami = "unsuitable",
            suitabilityLargeFire = "not-applicable",
            petAcceptance = "accepted",
            phoneNumber = "078-411-0556",
            note = "《洪水時》別の避難場所へ避難",
        )

    /** 屋外の緊急避難場所。電話番号と備考を持たない（元データの屋外 88 件に相当）。 */
    private val honjoPark =
        ShelterView(
            id = "kobe-099",
            name = "本庄中央公園",
            address = "神戸市東灘区甲南町1-1",
            latitude = 34.0,
            longitude = 135.0,
            type = "emergency",
            siting = "outdoor",
            suitabilityLandslide = "not-applicable",
            suitabilityFlood = "not-applicable",
            suitabilityTsunami = "suitable",
            suitabilityLargeFire = "suitable",
            petAcceptance = "not-accepted",
            phoneNumber = null,
            note = null,
        )

    private fun stub(
        language: Language,
        shelters: List<ShelterView>,
    ) {
        given(getShelterListService.getShelterList(language)).willReturn(ShelterListView(shelters, metadata))
    }

    @Test
    fun `lang=ja で Client EvacuationShelter 形の JSON を data に返す`() {
        stub(Language.JA, listOf(higashinadaElementary, honjoPark))

        mockMvc
            .perform(get("/api/v1/evacuation/shelters?lang=ja"))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith("application/json"))
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].id").value("kobe-001"))
            .andExpect(jsonPath("$.data[0].name").value("東灘小学校"))
            .andExpect(jsonPath("$.data[0].address").value("神戸市東灘区深江北町2-4-1"))
            .andExpect(jsonPath("$.data[0].coordinates.latitude").value(34.7248161))
            .andExpect(jsonPath("$.data[0].coordinates.longitude").value(135.2944292))
            .andExpect(jsonPath("$.data[0].type").value("both"))
            .andExpect(jsonPath("$.data[0].siting").value("indoor"))
            .andExpect(jsonPath("$.data[0].petAcceptance").value("accepted"))
            .andExpect(jsonPath("$.data[0].phoneNumber").value("078-411-0556"))
            .andExpect(jsonPath("$.data[0].note").value("《洪水時》別の避難場所へ避難"))
            // 適否は全種別を含む。△（conditional）を ○ に丸めない（#180）。
            .andExpect(jsonPath("$.data[0].suitability.landslide").value("suitable"))
            .andExpect(jsonPath("$.data[0].suitability.flood").value("conditional"))
            .andExpect(jsonPath("$.data[0].suitability.tsunami").value("unsuitable"))
            .andExpect(jsonPath("$.data[0].suitability['large-fire']").value("not-applicable"))
            .andExpect(jsonPath("$.data[1].siting").value("outdoor"))
            .andExpect(jsonPath("$.data[1].suitability['large-fire']").value("suitable"))
            .andExpect(jsonPath("$.data[1].phoneNumber").doesNotExist())
            .andExpect(jsonPath("$.data[1].note").doesNotExist())
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
}

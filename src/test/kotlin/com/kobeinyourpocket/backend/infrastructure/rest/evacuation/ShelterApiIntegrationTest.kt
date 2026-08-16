package com.kobeinyourpocket.backend.infrastructure.rest.evacuation

import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.model.EvacuationShelter
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.repository.ShelterRepository
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterCapacity
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterCoordinates
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterLocalization
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterLocalizations
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterMedia
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterType
import com.kobeinyourpocket.backend.domain.evacuation.shelterfacilitycategory.model.ShelterFacilityCategory
import com.kobeinyourpocket.backend.domain.user.vo.Role
import com.kobeinyourpocket.backend.infrastructure.persistence.evacuation.entity.ShelterDatasetMetadataEntity
import com.kobeinyourpocket.backend.infrastructure.persistence.evacuation.repository.ShelterDatasetMetadataJpaRepository
import com.kobeinyourpocket.backend.infrastructure.persistence.evacuation.repository.ShelterLocalizationJpaRepository
import com.kobeinyourpocket.backend.infrastructure.security.withRole
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 避難所一覧の統合テスト（#67）。controller → application → JPA → DB を実 Bean で通し、
 * `?lang=` 主・`Accept-Language` 従・en フォールバック（D1）と Client `EvacuationShelter` 形を end-to-end で検証する。
 *
 * データ投入は [ShelterRepository]（write port）で行う。削除 API（#144）の契約もここで検証する。
 * テスト環境は H2（`application-test.yml`、Hibernate create-drop / Flyway 無効）。
 * 各テストは [Transactional] でロールバックし相互に独立させる。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ShelterApiIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var shelterRepository: ShelterRepository

    @Autowired
    private lateinit var shelterDatasetMetadataJpaRepository: ShelterDatasetMetadataJpaRepository

    @Autowired
    private lateinit var shelterLocalizationJpaRepository: ShelterLocalizationJpaRepository

    private val metadata =
        ShelterDatasetMetadataEntity(
            id = ShelterDatasetMetadataEntity.SINGLETON_ID,
            source = "神戸市オープンデータポータル「神戸市避難場所」(CC BY 2.1 JP)",
            asOf = LocalDate.of(2025, 4, 2),
            updatedAt = Instant.parse("2025-04-02T00:00:00Z"),
        )

    private val kobeCityHall =
        EvacuationShelter.create(
            id = EvacuationShelter.Id.of("kobe-city-hall"),
            coordinates = ShelterCoordinates.of(34.6826, 135.1863),
            type = ShelterType.DUAL_USE,
            facilityCategory = ShelterFacilityCategory.GOVERNMENT,
            media = ShelterMedia("https://example.com/kobe-city-hall.webp"),
            accessible = true,
            localizations =
                ShelterLocalizations.of(
                    mapOf(
                        Language.JA to ShelterLocalization("神戸市役所", "兵庫県神戸市中央区加納町6丁目5-1"),
                        Language.EN to ShelterLocalization("Kobe City Hall", "6-5-1 Kanomachi, Chuo-ku, Kobe, Hyogo"),
                        Language.ZH to ShelterLocalization("神户市政府", "兵库县神户市中央区加纳町6丁目5-1"),
                    ),
                ),
            capacity = ShelterCapacity(500),
            externalUrl = "https://example.com/kobe-city-hall",
        )

    private val minimalShelter =
        EvacuationShelter.create(
            id = EvacuationShelter.Id.of("minimal-shelter"),
            coordinates = ShelterCoordinates.of(34.0, 135.0),
            type = ShelterType.DESIGNATED_EMERGENCY_EVACUATION_SITE,
            facilityCategory = ShelterFacilityCategory.PARK,
            media = ShelterMedia("https://example.com/minimal.webp"),
            accessible = false,
            localizations = ShelterLocalizations.of(mapOf(Language.EN to ShelterLocalization("Minimal Park", "Somewhere"))),
        )

    private fun seedShelters() {
        shelterRepository.save(kobeCityHall)
        shelterRepository.save(minimalShelter)
    }

    private fun seedMetadata() {
        shelterDatasetMetadataJpaRepository.save(metadata)
    }

    @Test
    fun `GET lang=ja で Client EvacuationShelter 形の一覧を data に返す`() {
        seedShelters()
        seedMetadata()

        mockMvc
            .perform(get("/api/v1/evacuation/shelters?lang=ja"))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].id").value("kobe-city-hall"))
            .andExpect(jsonPath("$.data[0].name").value("神戸市役所"))
            .andExpect(jsonPath("$.data[0].address").value("兵庫県神戸市中央区加納町6丁目5-1"))
            .andExpect(jsonPath("$.data[0].coordinates.latitude").value(34.6826))
            .andExpect(jsonPath("$.data[0].type").value("both"))
            .andExpect(jsonPath("$.data[0].facilityCategory").value("government"))
            .andExpect(jsonPath("$.data[0].media.imageUrl").value("https://example.com/kobe-city-hall.webp"))
            .andExpect(jsonPath("$.data[0].capacity").value(500))
            .andExpect(jsonPath("$.data[0].accessible").value(true))
            .andExpect(jsonPath("$.data[0].externalUrl").value("https://example.com/kobe-city-hall"))
            .andExpect(jsonPath("$.data[1].id").value("minimal-shelter"))
            .andExpect(jsonPath("$.data[1].capacity").doesNotExist())
            .andExpect(jsonPath("$.data[1].externalUrl").doesNotExist())
    }

    @Test
    fun `GET meta にデータセットの出典・データ基準日・最終更新日時を返す`() {
        seedShelters()
        seedMetadata()

        mockMvc
            .perform(get("/api/v1/evacuation/shelters?lang=ja"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.meta.source").value(metadata.source))
            .andExpect(jsonPath("$.meta.asOf").value("2025-04-02"))
            .andExpect(jsonPath("$.meta.updatedAt").value("2025-04-02T00:00:00Z"))
    }

    @Test
    fun `GET lang=zh で中国語ローカライズを返す`() {
        seedShelters()
        seedMetadata()

        mockMvc
            .perform(get("/api/v1/evacuation/shelters?lang=zh"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].name").value("神户市政府"))
    }

    @Test
    fun `要求言語のローカライズが無い避難所は en へフォールバックする`() {
        seedShelters()
        seedMetadata()

        // minimal-shelter は en のみ収録 → lang=ja でも en を返す
        mockMvc
            .perform(get("/api/v1/evacuation/shelters?lang=ja"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[1].name").value("Minimal Park"))
    }

    @Test
    fun `lang 未指定なら Accept-Language を従として解決する`() {
        seedShelters()
        seedMetadata()

        mockMvc
            .perform(get("/api/v1/evacuation/shelters").header("Accept-Language", "ja-JP,ja;q=0.9"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].name").value("神戸市役所"))
    }

    @Test
    fun `未対応の言語コードは en へフォールバックする`() {
        seedShelters()
        seedMetadata()

        // fr は非対応言語コード → Language.of が null を返し en（DEFAULT）で解決
        mockMvc
            .perform(get("/api/v1/evacuation/shelters?lang=fr"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].name").value("Kobe City Hall"))
    }

    @Test
    fun `データが無ければ data は空配列だが meta は返す`() {
        seedMetadata()

        mockMvc
            .perform(get("/api/v1/evacuation/shelters?lang=ja"))
            .andExpect(status().isOk)
            .andExpect(content().json("""{"data":[]}"""))
            .andExpect(jsonPath("$.meta.source").value(metadata.source))
    }

    @Test
    fun `DELETE は未認証だと 401`() {
        seedShelters()

        mockMvc
            .perform(delete("/api/v1/evacuation/shelters/kobe-city-hall"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `DELETE は一般ロールだと 403`() {
        seedShelters()

        mockMvc
            .perform(delete("/api/v1/evacuation/shelters/kobe-city-hall").with(withRole(Role.GENERAL)))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.status").value(403))
    }

    @Test
    fun `DELETE を運営ロールで実行すると 204 になり一覧から消える`() {
        seedShelters()
        seedMetadata()

        mockMvc
            .perform(delete("/api/v1/evacuation/shelters/kobe-city-hall").with(withRole(Role.OPERATOR)))
            .andExpect(status().isNoContent)

        mockMvc
            .perform(get("/api/v1/evacuation/shelters?lang=ja"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].id").value("minimal-shelter"))
    }

    @Test
    fun `DELETE は admin でも実行できる（ロール階層）`() {
        seedShelters()

        mockMvc
            .perform(delete("/api/v1/evacuation/shelters/kobe-city-hall").with(withRole(Role.ADMIN)))
            .andExpect(status().isNoContent)
    }

    @Test
    fun `DELETE が未登録なら 404 と統一エラー JSON を返す`() {
        seedShelters()

        mockMvc
            .perform(delete("/api/v1/evacuation/shelters/unknown-shelter").with(withRole(Role.OPERATOR)))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
    }

    @Test
    fun `DELETE はローカライズも消し孤児行を残さない`() {
        seedShelters()
        // kobe-city-hall は ja/en/zh の 3 件、minimal-shelter は en の 1 件
        assertEquals(4, shelterLocalizationJpaRepository.count())

        mockMvc
            .perform(delete("/api/v1/evacuation/shelters/kobe-city-hall").with(withRole(Role.OPERATOR)))
            .andExpect(status().isNoContent)

        assertEquals(1, shelterLocalizationJpaRepository.count())
    }
}

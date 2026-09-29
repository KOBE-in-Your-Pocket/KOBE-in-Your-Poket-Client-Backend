package com.kobeinyourpocket.backend.infrastructure.rest.evacuation

import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.model.EvacuationShelter
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.repository.ShelterRepository
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.PetAcceptance
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterCoordinates
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterLocalization
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterLocalizations
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterSiting
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterSuitabilities
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterSuitability
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterType
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
            source = "神戸市オープンデータ「指定緊急避難場所・指定避難所」(CC BY 4.0)",
            asOf = LocalDate.of(2025, 4, 2),
            updatedAt = Instant.parse("2025-04-02T00:00:00Z"),
        )

    private val higashinadaElementary =
        EvacuationShelter.create(
            id = EvacuationShelter.Id.of("kobe-001"),
            coordinates = ShelterCoordinates.of(34.7248161, 135.2944292),
            type = ShelterType.DUAL_USE,
            siting = ShelterSiting.INDOOR,
            suitabilities =
                ShelterSuitabilities.of(
                    landslide = ShelterSuitability.SUITABLE,
                    flood = ShelterSuitability.CONDITIONAL,
                    tsunami = ShelterSuitability.UNSUITABLE,
                    largeFire = ShelterSuitability.NOT_APPLICABLE,
                ),
            petAcceptance = PetAcceptance.ACCEPTED,
            localizations =
                ShelterLocalizations.of(
                    mapOf(
                        Language.JA to
                            ShelterLocalization("東灘小学校", "神戸市東灘区深江北町2-4-1", note = "《洪水時》別の避難場所へ避難"),
                        Language.EN to
                            ShelterLocalization("Higashinada Elementary School", "2-4-1 Fukaekitamachi, Higashinada-ku, Kobe"),
                        Language.ZH to ShelterLocalization("东滩小学", "神户市东滩区深江北町2-4-1"),
                    ),
                ),
            phoneNumber = "078-411-0556",
        )

    /** 屋外の緊急避難場所。en のみ収録し、電話番号・備考を持たない。 */
    private val honjoPark =
        EvacuationShelter.create(
            id = EvacuationShelter.Id.of("kobe-099"),
            coordinates = ShelterCoordinates.of(34.0, 135.0),
            type = ShelterType.DESIGNATED_EMERGENCY_EVACUATION_SITE,
            siting = ShelterSiting.OUTDOOR,
            suitabilities =
                ShelterSuitabilities.of(
                    landslide = ShelterSuitability.NOT_APPLICABLE,
                    flood = ShelterSuitability.NOT_APPLICABLE,
                    tsunami = ShelterSuitability.SUITABLE,
                    largeFire = ShelterSuitability.SUITABLE,
                ),
            petAcceptance = PetAcceptance.NOT_ACCEPTED,
            localizations = ShelterLocalizations.of(mapOf(Language.EN to ShelterLocalization("Honjo Park", "Somewhere"))),
        )

    private fun seedShelters() {
        shelterRepository.save(higashinadaElementary)
        shelterRepository.save(honjoPark)
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
            .andExpect(jsonPath("$.data[0].id").value("kobe-001"))
            .andExpect(jsonPath("$.data[0].name").value("東灘小学校"))
            .andExpect(jsonPath("$.data[0].address").value("神戸市東灘区深江北町2-4-1"))
            .andExpect(jsonPath("$.data[0].coordinates.latitude").value(34.7248161))
            .andExpect(jsonPath("$.data[0].type").value("both"))
            .andExpect(jsonPath("$.data[0].siting").value("indoor"))
            .andExpect(jsonPath("$.data[0].petAcceptance").value("accepted"))
            .andExpect(jsonPath("$.data[0].phoneNumber").value("078-411-0556"))
            .andExpect(jsonPath("$.data[0].note").value("《洪水時》別の避難場所へ避難"))
            .andExpect(jsonPath("$.data[0].suitability.landslide").value("suitable"))
            .andExpect(jsonPath("$.data[0].suitability.flood").value("conditional"))
            .andExpect(jsonPath("$.data[0].suitability.tsunami").value("unsuitable"))
            .andExpect(jsonPath("$.data[0].suitability['large-fire']").value("not-applicable"))
            .andExpect(jsonPath("$.data[1].id").value("kobe-099"))
            .andExpect(jsonPath("$.data[1].siting").value("outdoor"))
            .andExpect(jsonPath("$.data[1].suitability['large-fire']").value("suitable"))
            .andExpect(jsonPath("$.data[1].phoneNumber").doesNotExist())
            .andExpect(jsonPath("$.data[1].note").doesNotExist())
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
            .andExpect(jsonPath("$.data[0].name").value("东滩小学"))
    }

    @Test
    fun `要求言語のローカライズが無い避難所は en へフォールバックする`() {
        seedShelters()
        seedMetadata()

        // kobe-099 は en のみ収録 → lang=ja でも en を返す
        mockMvc
            .perform(get("/api/v1/evacuation/shelters?lang=ja"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[1].name").value("Honjo Park"))
    }

    @Test
    fun `lang 未指定なら Accept-Language を従として解決する`() {
        seedShelters()
        seedMetadata()

        mockMvc
            .perform(get("/api/v1/evacuation/shelters").header("Accept-Language", "ja-JP,ja;q=0.9"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].name").value("東灘小学校"))
    }

    @Test
    fun `未対応の言語コードは en へフォールバックする`() {
        seedShelters()
        seedMetadata()

        // fr は非対応言語コード → Language.of が null を返し en（DEFAULT）で解決
        mockMvc
            .perform(get("/api/v1/evacuation/shelters?lang=fr"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].name").value("Higashinada Elementary School"))
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
            .perform(delete("/api/v1/evacuation/shelters/kobe-001"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `DELETE は一般ロールだと 403`() {
        seedShelters()

        mockMvc
            .perform(delete("/api/v1/evacuation/shelters/kobe-001").with(withRole(Role.GENERAL)))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.status").value(403))
    }

    @Test
    fun `DELETE を運営ロールで実行すると 204 になり一覧から消える`() {
        seedShelters()
        seedMetadata()

        mockMvc
            .perform(delete("/api/v1/evacuation/shelters/kobe-001").with(withRole(Role.OPERATOR)))
            .andExpect(status().isNoContent)

        mockMvc
            .perform(get("/api/v1/evacuation/shelters?lang=ja"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].id").value("kobe-099"))
    }

    @Test
    fun `DELETE は admin でも実行できる（ロール階層）`() {
        seedShelters()

        mockMvc
            .perform(delete("/api/v1/evacuation/shelters/kobe-001").with(withRole(Role.ADMIN)))
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
        // kobe-001 は ja/en/zh の 3 件、kobe-099 は en の 1 件
        assertEquals(4, shelterLocalizationJpaRepository.count())

        mockMvc
            .perform(delete("/api/v1/evacuation/shelters/kobe-001").with(withRole(Role.OPERATOR)))
            .andExpect(status().isNoContent)

        assertEquals(1, shelterLocalizationJpaRepository.count())
    }
}

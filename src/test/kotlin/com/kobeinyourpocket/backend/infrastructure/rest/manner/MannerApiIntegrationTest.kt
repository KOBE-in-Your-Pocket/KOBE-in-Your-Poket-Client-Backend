package com.kobeinyourpocket.backend.infrastructure.rest.manner

import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.domain.manner.manneritem.model.MannerItem
import com.kobeinyourpocket.backend.domain.manner.manneritem.repository.MannerRepository
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerIcon
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerKind
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerLocalization
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerLocalizations
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerScope
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.RelatedSpotId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import kotlin.test.Test

/**
 * マナー一覧の統合テスト（#72）。controller → application → JPA → DB を実 Bean で通し、
 * `?lang=` 主・`Accept-Language` 従・en フォールバック（D1）と Client `MannerItem` 形を end-to-end で検証する。
 *
 * 書き込み API は無いため、データ投入は [MannerRepository]（write port）で行う。
 * テスト環境は H2（`application-test.yml`、Hibernate create-drop / Flyway 無効）。
 * 各テストは [Transactional] でロールバックし相互に独立させる。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MannerApiIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var mannerRepository: MannerRepository

    private val arimaOnsen =
        MannerItem.create(
            id = MannerItem.Id.of("arima-onsen-bathing"),
            icon = MannerIcon.of("hot-spring"),
            kind = MannerKind.MANNER,
            scope = MannerScope.LOCAL,
            localizations =
                MannerLocalizations.of(
                    mapOf(
                        Language.JA to MannerLocalization("有馬温泉の入浴マナー", "湯船に入る前にかけ湯で体を流しましょう。"),
                        Language.EN to
                            MannerLocalization(
                                "Arima Onsen bathing etiquette",
                                "Rinse your body before entering the bath.",
                            ),
                        Language.ZH to MannerLocalization("有马温泉入浴礼仪", "入浴前请先冲净身体。"),
                    ),
                ),
            relatedSpotIds = listOf(RelatedSpotId.of("arima-onsen")),
        )

    private val noLittering =
        MannerItem.create(
            id = MannerItem.Id.of("no-littering"),
            icon = MannerIcon.of("trash"),
            kind = MannerKind.RULE,
            scope = MannerScope.JAPAN,
            localizations =
                MannerLocalizations.of(
                    mapOf(
                        Language.EN to MannerLocalization("No littering", "Please carry your trash with you."),
                    ),
                ),
        )

    private fun seedItems() {
        mannerRepository.save(arimaOnsen)
        mannerRepository.save(noLittering)
    }

    @Test
    fun `GET lang=ja で Client MannerItem 形の一覧を返す`() {
        seedItems()

        mockMvc
            .perform(get("/api/v1/manner/items?lang=ja"))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value("arima-onsen-bathing"))
            .andExpect(jsonPath("$[0].title").value("有馬温泉の入浴マナー"))
            .andExpect(jsonPath("$[0].description").value("湯船に入る前にかけ湯で体を流しましょう。"))
            .andExpect(jsonPath("$[0].icon").value("hot-spring"))
            .andExpect(jsonPath("$[0].kind").value("manner"))
            .andExpect(jsonPath("$[0].scope").value("local"))
            .andExpect(jsonPath("$[0].relatedSpotIds[0]").value("arima-onsen"))
            .andExpect(jsonPath("$[1].id").value("no-littering"))
            .andExpect(jsonPath("$[1].kind").value("rule"))
            .andExpect(jsonPath("$[1].scope").value("japan"))
            .andExpect(jsonPath("$[1].relatedSpotIds.length()").value(0))
    }

    @Test
    fun `GET lang=zh で中国語ローカライズを返す`() {
        seedItems()

        mockMvc
            .perform(get("/api/v1/manner/items?lang=zh"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].title").value("有马温泉入浴礼仪"))
    }

    @Test
    fun `要求言語のローカライズが無い項目は en へフォールバックする`() {
        seedItems()

        // no-littering は en のみ収録 → lang=ja でも en を返す
        mockMvc
            .perform(get("/api/v1/manner/items?lang=ja"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[1].title").value("No littering"))
    }

    @Test
    fun `lang 未指定なら Accept-Language を従として解決する`() {
        seedItems()

        mockMvc
            .perform(get("/api/v1/manner/items").header("Accept-Language", "ja-JP,ja;q=0.9"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].title").value("有馬温泉の入浴マナー"))
    }

    @Test
    fun `未対応の言語コードは en へフォールバックする`() {
        seedItems()

        // fr は非対応言語コード → Language.of が null を返し en（DEFAULT）で解決
        mockMvc
            .perform(get("/api/v1/manner/items?lang=fr"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].title").value("Arima Onsen bathing etiquette"))
    }

    @Test
    fun `データが無ければ空配列を返す`() {
        mockMvc
            .perform(get("/api/v1/manner/items?lang=ja"))
            .andExpect(status().isOk)
            .andExpect(content().json("[]"))
    }
}

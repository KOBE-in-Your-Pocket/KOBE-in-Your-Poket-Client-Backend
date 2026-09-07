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
import com.kobeinyourpocket.backend.domain.user.vo.Role
import com.kobeinyourpocket.backend.infrastructure.security.withRole
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import kotlin.test.Test

/**
 * マナー一覧の統合テスト（#72）。controller → application → JPA → DB を実 Bean で通し、
 * `?lang=` 主・`Accept-Language` 従・en フォールバック（D1）と Client `MannerItem` 形を end-to-end で検証する。
 *
 * 読み取りテストのデータ投入は [MannerRepository]（write port）で行う。書き込み API 自体の
 * テストは末尾にまとめてある（ロール・採番・バリデーション）。
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

    // ── 書き込み API ────────────────────────────────────────────────

    /** 全言語そろった登録リクエストの JSON。 */
    private fun requestJson(
        en: String = "No littering",
        icon: String? = "trash",
        iconUrl: String? = null,
        relatedSpotIds: List<String> = emptyList(),
        languages: List<Language> = Language.entries,
    ): String {
        val localizations =
            languages.joinToString(",") { language ->
                val title = if (language == Language.EN) en else "${language.code} タイトル"
                """"${language.code}": {"title": "$title", "description": "${language.code} 説明"}"""
            }
        val spots = relatedSpotIds.joinToString(",") { "\"$it\"" }
        return """
            {
              "icon": ${if (icon == null) "null" else "\"$icon\""},
              "iconUrl": ${if (iconUrl == null) "null" else "\"$iconUrl\""},
              "kind": "rule",
              "scope": "japan",
              "relatedSpotIds": [$spots],
              "localizations": {$localizations}
            }
            """.trimIndent()
    }

    @Test
    fun `POST は英語タイトルから id を採番して 201 を返す`() {
        mockMvc
            .perform(
                post("/api/v1/manner/items")
                    .with(withRole(Role.OPERATOR))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson(en = "No littering", relatedSpotIds = listOf("mount-rokko"))),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value("no-littering"))
            .andExpect(jsonPath("$.kind").value("rule"))
            .andExpect(jsonPath("$.relatedSpotIds[0]").value("mount-rokko"))
            .andExpect(jsonPath("$.localizations.ja.title").value("ja タイトル"))
    }

    @Test
    fun `POST はアイコン画像だけでも登録できる`() {
        mockMvc
            .perform(
                post("/api/v1/manner/items")
                    .with(withRole(Role.OPERATOR))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson(icon = null, iconUrl = "https://example.com/icons/no-littering.png")),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.icon").doesNotExist())
            .andExpect(jsonPath("$.iconUrl").value("https://example.com/icons/no-littering.png"))
    }

    @Test
    fun `POST は対応言語が欠けていれば 400`() {
        mockMvc
            .perform(
                post("/api/v1/manner/items")
                    .with(withRole(Role.OPERATOR))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson(languages = listOf(Language.EN, Language.JA))),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `POST は英語タイトルから id を作れなければ 400`() {
        mockMvc
            .perform(
                post("/api/v1/manner/items")
                    .with(withRole(Role.OPERATOR))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson(en = "---")),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `POST は運営ロールでなければ 403`() {
        mockMvc
            .perform(
                post("/api/v1/manner/items")
                    .with(withRole(Role.GENERAL))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson()),
            ).andExpect(status().isForbidden)
    }

    @Test
    fun `PUT は内容を差し替えるが id は変えない`() {
        mannerRepository.save(arimaOnsen)

        mockMvc
            .perform(
                put("/api/v1/manner/items/arima-onsen-bathing")
                    .with(withRole(Role.OPERATOR))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson(en = "Completely different title")),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value("arima-onsen-bathing"))
            .andExpect(jsonPath("$.localizations.en.title").value("Completely different title"))
    }

    @Test
    fun `PUT は対象が無ければ 404`() {
        mockMvc
            .perform(
                put("/api/v1/manner/items/missing")
                    .with(withRole(Role.OPERATOR))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson()),
            ).andExpect(status().isNotFound)
    }

    @Test
    fun `DELETE は 204 を返し一覧から消える`() {
        mannerRepository.save(arimaOnsen)

        mockMvc
            .perform(delete("/api/v1/manner/items/arima-onsen-bathing").with(withRole(Role.OPERATOR)))
            .andExpect(status().isNoContent)

        mockMvc
            .perform(get("/api/v1/manner/items?lang=ja"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    fun `DELETE は対象が無ければ 404`() {
        mockMvc
            .perform(delete("/api/v1/manner/items/missing").with(withRole(Role.OPERATOR)))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `DELETE は運営ロールでなければ 403`() {
        mannerRepository.save(arimaOnsen)

        mockMvc
            .perform(delete("/api/v1/manner/items/arima-onsen-bathing").with(withRole(Role.GENERAL)))
            .andExpect(status().isForbidden)
    }
}

package com.kobeinyourpocket.backend.infrastructure.rest.tourism

import com.kobeinyourpocket.backend.application.tourism.command.RegisterSpotService
import com.kobeinyourpocket.backend.application.tourism.query.GetSpotService
import com.kobeinyourpocket.backend.application.tourism.query.ListSpotsService
import com.kobeinyourpocket.backend.application.tourism.query.SpotNotFoundException
import com.kobeinyourpocket.backend.application.tourism.query.SpotView
import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.domain.tourism.spot.model.Spot
import com.kobeinyourpocket.backend.domain.tourism.spot.model.SpotWithLocalizations
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.Coordinates
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.Genre
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotLocalization
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotLocalizations
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotMedia
import com.kobeinyourpocket.backend.infrastructure.rest.common.GlobalExceptionHandler
import org.hamcrest.Matchers.containsString
import org.mockito.BDDMockito.given
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.Test

@WebMvcTest(SpotController::class)
@Import(GlobalExceptionHandler::class)
class SpotControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var listSpotsService: ListSpotsService

    @MockitoBean
    private lateinit var getSpotService: GetSpotService

    @MockitoBean
    private lateinit var registerSpotService: RegisterSpotService

    private val localizations =
        SpotLocalizations.of(
            mapOf(
                Language.JA to
                    SpotLocalization(
                        "神戸ポートタワー",
                        "ランドマーク",
                        "神戸のシンボル。",
                        "9:00-23:00",
                        "神戸市中央区波止場町5-5",
                    ),
                Language.EN to
                    SpotLocalization(
                        "Kobe Port Tower",
                        "Landmark",
                        "The symbol of Kobe.",
                        "9:00-23:00",
                        "5-5 Hatobacho, Chuo-ku, Kobe",
                    ),
                Language.ZH to
                    SpotLocalization(
                        "神户港塔",
                        "地标",
                        "神户的象征。",
                        "9:00-23:00",
                        "神户市中央区波止场町5-5",
                    ),
                Language.KO to
                    SpotLocalization(
                        "고베 포트 타워",
                        "랜드마크",
                        "고베의 상징.",
                        "9:00-23:00",
                        "고베시 주오구 하토바초 5-5",
                    ),
            ),
        )

    private val createdSpot =
        SpotWithLocalizations(
            spot =
                Spot(
                    id = SpotId.of("new-spot-id"),
                    genre = Genre.LANDMARK,
                    coordinates = Coordinates.of(34.6826, 135.1863),
                    media = SpotMedia("https://example.com/kobe-port-tower.webp"),
                    rating = null,
                ),
            localizations = localizations,
        )

    private val registerBody =
        """
        {
          "genre": "landmark",
          "coordinates": { "latitude": 34.6826, "longitude": 135.1863 },
          "imageUrl": "https://example.com/kobe-port-tower.webp",
          "localizations": {
            "ja": {
              "name": "神戸ポートタワー",
              "categoryLabel": "ランドマーク",
              "description": "神戸のシンボル。",
              "businessHours": "9:00-23:00",
              "address": "神戸市中央区波止場町5-5"
            },
            "en": {
              "name": "Kobe Port Tower",
              "categoryLabel": "Landmark",
              "description": "The symbol of Kobe.",
              "businessHours": "9:00-23:00",
              "address": "5-5 Hatobacho, Chuo-ku, Kobe"
            },
            "zh": {
              "name": "神户港塔",
              "categoryLabel": "地标",
              "description": "神户的象征。",
              "businessHours": "9:00-23:00",
              "address": "神户市中央区波止场町5-5"
            },
            "ko": {
              "name": "고베 포트 타워",
              "categoryLabel": "랜드마크",
              "description": "고베의 상징.",
              "businessHours": "9:00-23:00",
              "address": "고베시 주오구 하토바초 5-5"
            }
          }
        }
        """.trimIndent()

    private val portTower =
        SpotView(
            id = "kobe-port-tower",
            name = "神戸ポートタワー",
            genre = "landmark",
            description = "神戸のシンボル。",
            latitude = 34.6826,
            longitude = 135.1863,
            businessHours = "9:00-23:00",
            categoryLabel = "ランドマーク",
            imageUrl = "https://example.com/kobe-port-tower.webp",
            rating = 4.5,
            address = "神戸市中央区波止場町5-5",
        )

    private val noRating = portTower.copy(id = "no-rating", rating = null)

    @Test
    fun `lang=ja でモック互換 JSON を返す`() {
        given(listSpotsService.listSpots(Language.JA)).willReturn(listOf(portTower, noRating))

        mockMvc
            .perform(get("/api/v1/tourism/spots?lang=ja"))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith("application/json"))
            .andExpect(jsonPath("$[0].id").value("kobe-port-tower"))
            .andExpect(jsonPath("$[0].name").value("神戸ポートタワー"))
            .andExpect(jsonPath("$[0].genre").value("landmark"))
            .andExpect(jsonPath("$[0].address").value("神戸市中央区波止場町5-5"))
            .andExpect(jsonPath("$[0].rating.value").value(4.5))
            .andExpect(jsonPath("$[1].rating").doesNotExist())
    }

    @Test
    fun `lang クエリを主として言語解決する`() {
        given(listSpotsService.listSpots(Language.EN)).willReturn(emptyList())

        mockMvc.perform(get("/api/v1/tourism/spots?lang=en")).andExpect(status().isOk)

        verify(listSpotsService).listSpots(Language.EN)
    }

    @Test
    fun `lang 未指定なら Accept-Language を従として解決する`() {
        given(listSpotsService.listSpots(Language.KO)).willReturn(emptyList())

        mockMvc
            .perform(get("/api/v1/tourism/spots").header("Accept-Language", "ko-KR,ko;q=0.9,en;q=0.8"))
            .andExpect(status().isOk)

        verify(listSpotsService).listSpots(Language.KO)
    }

    @Test
    fun `lang も Accept-Language も無ければ en へフォールバックする`() {
        given(listSpotsService.listSpots(Language.EN)).willReturn(emptyList())

        mockMvc.perform(get("/api/v1/tourism/spots")).andExpect(status().isOk)

        verify(listSpotsService).listSpots(Language.EN)
    }

    @Test
    fun `GET id で該当スポットを取得できる`() {
        given(getSpotService.getSpot(SpotId.of("kobe-port-tower"), Language.JA)).willReturn(portTower)

        mockMvc
            .perform(get("/api/v1/tourism/spots/kobe-port-tower?lang=ja"))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith("application/json"))
            .andExpect(jsonPath("$.id").value("kobe-port-tower"))
            .andExpect(jsonPath("$.name").value("神戸ポートタワー"))
            .andExpect(jsonPath("$.address").value("神戸市中央区波止場町5-5"))
            .andExpect(jsonPath("$.rating.value").value(4.5))
    }

    @Test
    fun `GET id が未収録なら 404 と統一エラー JSON を返す`() {
        given(getSpotService.getSpot(SpotId.of("unknown-spot"), Language.JA))
            .willThrow(SpotNotFoundException(SpotId.of("unknown-spot")))

        mockMvc
            .perform(get("/api/v1/tourism/spots/unknown-spot?lang=ja"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("Spot not found: unknown-spot"))
    }

    @Test
    fun `POST でピン登録し 201 とモック互換 Spot JSON を返す`() {
        given(
            registerSpotService.registerSpot(
                Genre.LANDMARK,
                Coordinates.of(34.6826, 135.1863),
                SpotMedia("https://example.com/kobe-port-tower.webp"),
                localizations,
            ),
        ).willReturn(createdSpot)

        mockMvc
            .perform(
                post("/api/v1/tourism/spots")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(registerBody),
            ).andExpect(status().isCreated)
            .andExpect(content().contentTypeCompatibleWith("application/json"))
            .andExpect(jsonPath("$.id").value("new-spot-id"))
            .andExpect(jsonPath("$.name").value("Kobe Port Tower"))
            .andExpect(jsonPath("$.genre").value("landmark"))
            .andExpect(jsonPath("$.coordinates.latitude").value(34.6826))
            .andExpect(jsonPath("$.address").value("5-5 Hatobacho, Chuo-ku, Kobe"))
            .andExpect(jsonPath("$.media.imageUrl").value("https://example.com/kobe-port-tower.webp"))
            .andExpect(jsonPath("$.rating").doesNotExist())

        verify(registerSpotService).registerSpot(
            Genre.LANDMARK,
            Coordinates.of(34.6826, 135.1863),
            SpotMedia("https://example.com/kobe-port-tower.webp"),
            localizations,
        )
    }

    @Test
    fun `POST genre が空なら 400 と統一エラー JSON を返す`() {
        mockMvc
            .perform(
                post("/api/v1/tourism/spots")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "genre": "",
                          "coordinates": { "latitude": 34.6826, "longitude": 135.1863 },
                          "imageUrl": "https://example.com/x.webp",
                          "localizations": {
                            "ja": {
                              "name": "テスト",
                              "categoryLabel": "カテゴリ",
                              "description": "説明",
                              "businessHours": "9:00-17:00",
                              "address": "神戸市中央区1-1"
                            }
                          }
                        }
                        """.trimIndent(),
                    ),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.violations[0].field").value("genre"))

        verifyNoInteractions(registerSpotService)
    }

    @Test
    fun `POST JSON が壊れていれば 400 を返す`() {
        mockMvc
            .perform(
                post("/api/v1/tourism/spots")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{ invalid json"),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").exists())

        verifyNoInteractions(registerSpotService)
    }

    @Test
    fun `POST に4言語揃っていなければ 400 とエラーメッセージを返す`() {
        mockMvc
            .perform(
                post("/api/v1/tourism/spots")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "genre": "landmark",
                          "coordinates": { "latitude": 34.6826, "longitude": 135.1863 },
                          "imageUrl": "https://example.com/x.webp",
                          "localizations": {
                            "en": {
                              "name": "Tower",
                              "categoryLabel": "Landmark",
                              "description": "Desc",
                              "businessHours": "9:00-17:00",
                              "address": "1-1 Chuo-ku, Kobe"
                            }
                          }
                        }
                        """.trimIndent(),
                    ),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value(containsString("localizations")))

        verifyNoInteractions(registerSpotService)
    }
}

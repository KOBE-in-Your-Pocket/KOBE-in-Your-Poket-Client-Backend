package com.kobeinyourpocket.backend.infrastructure.rest.tourism

import com.kobeinyourpocket.backend.application.tourism.command.DeleteSpotService
import com.kobeinyourpocket.backend.application.tourism.command.PostReviewService
import com.kobeinyourpocket.backend.application.tourism.command.RegisterSpotService
import com.kobeinyourpocket.backend.application.tourism.command.UpdateReviewService
import com.kobeinyourpocket.backend.application.tourism.command.UpdateSpotService
import com.kobeinyourpocket.backend.application.tourism.query.GetSpotService
import com.kobeinyourpocket.backend.application.tourism.query.ListReviewsService
import com.kobeinyourpocket.backend.application.tourism.query.ListSpotsService
import com.kobeinyourpocket.backend.application.tourism.query.SpotView
import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.Test

/**
 * 全 lang-aware エンドポイントで共通 `LanguageResolver`（`?lang=` 主・`Accept-Language` 従・
 * 無指定/非対応コードは `Language.DEFAULT`=en へフォールバック）の解決契約を横断検証する
 * 契約テスト（要件定義 §8.1 / #74 / #75）。
 *
 * evacuation / manner はまだ REST エンドポイントが無い（`infrastructure/rest/{evacuation,manner}`
 * は空 placeholder）ため、現時点では実装済みの tourism の lang-aware エンドポイント
 * （spots 一覧・取得、reviews 一覧）を対象にする。新たに lang-aware エンドポイントを追加する際は、
 * 本テストにも同じ4パターン（lang優先／Accept-Language従／非対応langはAccept-Languageへ／
 * 無指定はenへ）を追加すること。
 */
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(controllers = [SpotController::class, ReviewController::class])
@Import(GlobalExceptionHandler::class)
class LanguageResolutionContractTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var listSpotsService: ListSpotsService

    @MockitoBean
    private lateinit var getSpotService: GetSpotService

    @MockitoBean
    private lateinit var registerSpotService: RegisterSpotService

    @MockitoBean
    private lateinit var deleteSpotService: DeleteSpotService

    @MockitoBean
    private lateinit var updateSpotService: UpdateSpotService

    @MockitoBean
    private lateinit var listReviewsService: ListReviewsService

    @MockitoBean
    private lateinit var postReviewService: PostReviewService

    @MockitoBean
    private lateinit var updateReviewService: UpdateReviewService

    private val spotId = SpotId.of("kobe-port-tower")

    // GET /api/v1/tourism/spots ------------------------------------------------------------

    @Test
    fun `GET spots一覧 lang を優先して解決する`() {
        given(listSpotsService.listSpots(Language.EN)).willReturn(emptyList())

        mockMvc
            .perform(get("/api/v1/tourism/spots?lang=en").header("Accept-Language", "ja"))
            .andExpect(status().isOk)

        verify(listSpotsService).listSpots(Language.EN)
    }

    @Test
    fun `GET spots一覧 lang が無ければ Accept-Language を解決する`() {
        given(listSpotsService.listSpots(Language.KO)).willReturn(emptyList())

        mockMvc
            .perform(get("/api/v1/tourism/spots").header("Accept-Language", "ko-KR,ko;q=0.9,en;q=0.8"))
            .andExpect(status().isOk)

        verify(listSpotsService).listSpots(Language.KO)
    }

    @Test
    fun `GET spots一覧 非対応 lang は Accept-Language へフォールバックする`() {
        given(listSpotsService.listSpots(Language.ZH)).willReturn(emptyList())

        mockMvc
            .perform(get("/api/v1/tourism/spots?lang=fr").header("Accept-Language", "zh-CN"))
            .andExpect(status().isOk)

        verify(listSpotsService).listSpots(Language.ZH)
    }

    @Test
    fun `GET spots一覧 lang も Accept-Language も無ければ en へフォールバックする`() {
        given(listSpotsService.listSpots(Language.EN)).willReturn(emptyList())

        mockMvc.perform(get("/api/v1/tourism/spots")).andExpect(status().isOk)

        verify(listSpotsService).listSpots(Language.EN)
    }

    // GET /api/v1/tourism/spots/{id} --------------------------------------------------------

    @Test
    fun `GET spot取得 lang を優先して解決する`() {
        given(getSpotService.getSpot(spotId, Language.EN)).willReturn(spotViewOf(Language.EN))

        mockMvc
            .perform(get("/api/v1/tourism/spots/$spotId?lang=en").header("Accept-Language", "ja"))
            .andExpect(status().isOk)

        verify(getSpotService).getSpot(spotId, Language.EN)
    }

    @Test
    fun `GET spot取得 lang が無ければ Accept-Language を解決する`() {
        given(getSpotService.getSpot(spotId, Language.KO)).willReturn(spotViewOf(Language.KO))

        mockMvc
            .perform(get("/api/v1/tourism/spots/$spotId").header("Accept-Language", "ko-KR,ko;q=0.9,en;q=0.8"))
            .andExpect(status().isOk)

        verify(getSpotService).getSpot(spotId, Language.KO)
    }

    @Test
    fun `GET spot取得 非対応 lang は Accept-Language へフォールバックする`() {
        given(getSpotService.getSpot(spotId, Language.ZH)).willReturn(spotViewOf(Language.ZH))

        mockMvc
            .perform(get("/api/v1/tourism/spots/$spotId?lang=fr").header("Accept-Language", "zh-CN"))
            .andExpect(status().isOk)

        verify(getSpotService).getSpot(spotId, Language.ZH)
    }

    @Test
    fun `GET spot取得 lang も Accept-Language も無ければ en へフォールバックする`() {
        given(getSpotService.getSpot(spotId, Language.EN)).willReturn(spotViewOf(Language.EN))

        mockMvc.perform(get("/api/v1/tourism/spots/$spotId")).andExpect(status().isOk)

        verify(getSpotService).getSpot(spotId, Language.EN)
    }

    // GET /api/v1/tourism/spots/{spotId}/reviews --------------------------------------------

    @Test
    fun `GET reviews一覧 lang を優先して解決する`() {
        given(listReviewsService.listReviews(spotId, Language.EN)).willReturn(emptyList())

        mockMvc
            .perform(get("/api/v1/tourism/spots/$spotId/reviews?lang=en").header("Accept-Language", "ja"))
            .andExpect(status().isOk)

        verify(listReviewsService).listReviews(spotId, Language.EN)
    }

    @Test
    fun `GET reviews一覧 lang が無ければ Accept-Language を解決する`() {
        given(listReviewsService.listReviews(spotId, Language.KO)).willReturn(emptyList())

        mockMvc
            .perform(get("/api/v1/tourism/spots/$spotId/reviews").header("Accept-Language", "ko-KR,ko;q=0.9,en;q=0.8"))
            .andExpect(status().isOk)

        verify(listReviewsService).listReviews(spotId, Language.KO)
    }

    @Test
    fun `GET reviews一覧 非対応 lang は Accept-Language へフォールバックする`() {
        given(listReviewsService.listReviews(spotId, Language.ZH)).willReturn(emptyList())

        mockMvc
            .perform(get("/api/v1/tourism/spots/$spotId/reviews?lang=fr").header("Accept-Language", "zh-CN"))
            .andExpect(status().isOk)

        verify(listReviewsService).listReviews(spotId, Language.ZH)
    }

    @Test
    fun `GET reviews一覧 lang も Accept-Language も無ければ en へフォールバックする`() {
        given(listReviewsService.listReviews(spotId, Language.EN)).willReturn(emptyList())

        mockMvc.perform(get("/api/v1/tourism/spots/$spotId/reviews")).andExpect(status().isOk)

        verify(listReviewsService).listReviews(spotId, Language.EN)
    }

    private fun spotViewOf(language: Language) =
        SpotView(
            id = spotId.value,
            name = "Kobe Port Tower (${language.code})",
            genre = "landmark",
            description = "desc",
            latitude = 34.6826,
            longitude = 135.1863,
            businessHours = "9:00-23:00",
            categoryLabel = "Landmark",
            imageUrl = "https://example.com/kobe-port-tower.webp",
            rating = null,
            address = "5-5 Hatobacho, Chuo-ku, Kobe",
        )
}

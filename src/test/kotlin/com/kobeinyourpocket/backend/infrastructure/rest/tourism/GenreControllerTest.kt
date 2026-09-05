package com.kobeinyourpocket.backend.infrastructure.rest.tourism

import com.kobeinyourpocket.backend.application.tourism.GenreInUseException
import com.kobeinyourpocket.backend.application.tourism.GenreNotFoundException
import com.kobeinyourpocket.backend.application.tourism.command.DeleteGenreService
import com.kobeinyourpocket.backend.application.tourism.command.RegisterGenreService
import com.kobeinyourpocket.backend.application.tourism.command.UpdateGenreService
import com.kobeinyourpocket.backend.application.tourism.query.GenreView
import com.kobeinyourpocket.backend.application.tourism.query.ListGenresService
import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.domain.tourism.genre.model.Genre
import com.kobeinyourpocket.backend.domain.tourism.genre.vo.GenreCode
import com.kobeinyourpocket.backend.domain.tourism.genre.vo.GenreLocalizations
import com.kobeinyourpocket.backend.infrastructure.rest.common.GlobalExceptionHandler
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.willThrow
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.Test

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(GenreController::class)
@Import(GlobalExceptionHandler::class)
class GenreControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var listGenresService: ListGenresService

    @MockitoBean
    private lateinit var registerGenreService: RegisterGenreService

    @MockitoBean
    private lateinit var updateGenreService: UpdateGenreService

    @MockitoBean
    private lateinit var deleteGenreService: DeleteGenreService

    private val onsenView =
        GenreView(
            code = "onsen",
            displayOrder = 5,
            labels = mapOf("ja" to "温泉", "en" to "Hot Spring", "ko" to "온천", "zh" to "温泉"),
            spotCount = 3,
        )

    private val nightViewLabels =
        GenreLocalizations.of(
            mapOf(
                Language.JA to "夜景",
                Language.EN to "Night View",
                Language.KO to "야경",
                Language.ZH to "夜景",
            ),
        )

    private val nightViewGenre =
        Genre(
            code = GenreCode.of("night-view"),
            displayOrder = 6,
            localizations = nightViewLabels,
        )

    private val validBody =
        """
        {"displayOrder":6,"labels":{"ja":"夜景","en":"Night View","ko":"야경","zh":"夜景"}}
        """.trimIndent()

    @Test
    fun `一覧は全言語のラベルとスポット件数を返す`() {
        given(listGenresService.listGenres()).willReturn(listOf(onsenView))

        mockMvc
            .perform(get("/api/v1/tourism/genres"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].code").value("onsen"))
            .andExpect(jsonPath("$[0].displayOrder").value(5))
            .andExpect(jsonPath("$[0].labels.ja").value("温泉"))
            .andExpect(jsonPath("$[0].labels.en").value("Hot Spring"))
            .andExpect(jsonPath("$[0].spotCount").value(3))
    }

    @Test
    fun `登録すると生成された code を返す`() {
        given(registerGenreService.registerGenre(6, nightViewLabels)).willReturn(nightViewGenre)

        mockMvc
            .perform(post("/api/v1/tourism/genres").contentType(MediaType.APPLICATION_JSON).content(validBody))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.code").value("night-view"))
            .andExpect(jsonPath("$.labels.ja").value("夜景"))
    }

    @Test
    fun `表示名が全言語そろっていなければ 400`() {
        val missingKo = """{"displayOrder":1,"labels":{"ja":"夜景","en":"Night View","zh":"夜景"}}"""

        mockMvc
            .perform(post("/api/v1/tourism/genres").contentType(MediaType.APPLICATION_JSON).content(missingKo))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `更新はパスの code を使う`() {
        given(updateGenreService.updateGenre(GenreCode.of("night-view"), 6, nightViewLabels)).willReturn(nightViewGenre)

        mockMvc
            .perform(put("/api/v1/tourism/genres/night-view").contentType(MediaType.APPLICATION_JSON).content(validBody))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("night-view"))
    }

    @Test
    fun `削除は 204 を返す`() {
        mockMvc
            .perform(delete("/api/v1/tourism/genres/night-view"))
            .andExpect(status().isNoContent)

        verify(deleteGenreService).deleteGenre(GenreCode.of("night-view"))
    }

    @Test
    fun `使用中のジャンルの削除は 409`() {
        willThrow(GenreInUseException("onsen", 3))
            .given(deleteGenreService)
            .deleteGenre(GenreCode.of("onsen"))

        mockMvc
            .perform(delete("/api/v1/tourism/genres/onsen"))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.status").value(409))
    }

    @Test
    fun `存在しないジャンルの削除は 404`() {
        willThrow(GenreNotFoundException("unknown"))
            .given(deleteGenreService)
            .deleteGenre(GenreCode.of("unknown"))

        mockMvc
            .perform(delete("/api/v1/tourism/genres/unknown"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `code の形式が不正なら 400`() {
        mockMvc
            .perform(delete("/api/v1/tourism/genres/Night_View"))
            .andExpect(status().isBadRequest)
    }
}

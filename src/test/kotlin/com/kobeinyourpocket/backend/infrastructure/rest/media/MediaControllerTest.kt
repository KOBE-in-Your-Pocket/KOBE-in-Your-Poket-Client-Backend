package com.kobeinyourpocket.backend.infrastructure.rest.media

import com.kobeinyourpocket.backend.application.media.command.UploadMediaService
import com.kobeinyourpocket.backend.infrastructure.rest.common.GlobalExceptionHandler
import org.mockito.ArgumentMatchers
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.Test

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(MediaController::class)
@Import(GlobalExceptionHandler::class)
class MediaControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var uploadMediaService: UploadMediaService

    /** ByteArray 用の any マッチャ（mockito-kotlin 非導入のため薄いラッパ）。 */
    private fun anyBytes(): ByteArray = ArgumentMatchers.any(ByteArray::class.java) ?: ByteArray(0)

    @Test
    fun `画像をアップロードすると 201 と imageUrl を返す`() {
        given(uploadMediaService.upload(anyBytes(), ArgumentMatchers.anyString()))
            .willReturn("https://cdn.example.com/uploads/abc.jpg")

        val file = MockMultipartFile("file", "photo.jpg", "image/jpeg", byteArrayOf(1, 2, 3))

        mockMvc
            .perform(multipart("/api/v1/media/uploads").file(file))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.imageUrl").value("https://cdn.example.com/uploads/abc.jpg"))
    }

    @Test
    fun `未対応の content-type はサービスの拒否で 400 になる`() {
        given(uploadMediaService.upload(anyBytes(), ArgumentMatchers.anyString()))
            .willThrow(IllegalArgumentException("unsupported content type: application/pdf"))

        val file = MockMultipartFile("file", "doc.pdf", "application/pdf", byteArrayOf(1))

        mockMvc
            .perform(multipart("/api/v1/media/uploads").file(file))
            .andExpect(status().isBadRequest)
    }
}

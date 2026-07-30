package com.kobeinyourpocket.backend.infrastructure.rest.media

import com.kobeinyourpocket.backend.application.media.UploadMediaService
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

/**
 * 画像アップロード（代理保存 / #86）。
 *
 * 選択された画像を受け取り S3 に保存して公開 URL を返す。運営（operator / admin）限定。
 * 保存した URL を Client がスポット等の imageUrl として登録に使う。
 */
@RestController
@RequestMapping("/api/v1/media/uploads")
class MediaController(
    private val uploadMediaService: UploadMediaService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    fun upload(
        @RequestParam("file") file: MultipartFile,
    ): MediaUploadResponse {
        val url =
            uploadMediaService.upload(
                bytes = file.bytes,
                contentType = file.contentType,
            )
        return MediaUploadResponse(imageUrl = url)
    }
}

data class MediaUploadResponse(
    val imageUrl: String,
)

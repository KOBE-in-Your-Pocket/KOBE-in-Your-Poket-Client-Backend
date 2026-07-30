package com.kobeinyourpocket.backend.application.media.command

import com.kobeinyourpocket.backend.application.media.MediaStorage
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.util.unit.DataSize
import java.util.UUID

/**
 * 画像アップロードユースケース（write）。
 *
 * 申告された content-type は信用せず、**先頭バイト（magic number）から実際の画像形式を判定**する。
 * 任意バイト列を image と偽った投稿を弾き、保存・公開 URL は検出した実形式に基づく。
 * 不正入力は [IllegalArgumentException]（REST では 400）。
 */
@Service
class UploadMediaService(
    private val mediaStorage: MediaStorage,
    @param:Value("\${spring.servlet.multipart.max-file-size}") maxFileSize: DataSize,
) {
    // HTTP 境界（multipart 上限）と同じ設定値を単一ソースにする（設定変更時の分岐を防ぐ）。
    private val maxBytes: Long = maxFileSize.toBytes()

    /**
     * @param bytes ファイルのバイト列
     * @param contentType リクエストの content-type（検証は magic bytes 主。申告との食い違いは拒否）
     */
    fun upload(
        bytes: ByteArray,
        contentType: String?,
    ): String {
        require(bytes.isNotEmpty()) { "empty file" }
        require(bytes.size.toLong() <= maxBytes) { "file too large" }

        // 実体（magic bytes）で判定する。申告 MIME だけを信用しない。
        val detected =
            requireNotNull(detectImageType(bytes)) {
                "file content is not a supported image (jpeg / png / webp / gif)"
            }

        // 画像 MIME を申告しているのに実体と食い違う投稿は拒否する（なりすまし対策）。
        val declared = contentType?.substringBefore(';')?.trim()?.lowercase()
        if (declared != null && declared in IMAGE_CONTENT_TYPES && declared != detected.contentType) {
            throw IllegalArgumentException(
                "declared content type ($declared) does not match file content (${detected.contentType})",
            )
        }

        val key = "$KEY_PREFIX/${UUID.randomUUID()}.${detected.extension}"
        return mediaStorage.store(key = key, bytes = bytes, contentType = detected.contentType)
    }

    private data class ImageType(
        val contentType: String,
        val extension: String,
    )

    companion object {
        const val KEY_PREFIX: String = "uploads"

        private val IMAGE_CONTENT_TYPES =
            setOf("image/jpeg", "image/png", "image/webp", "image/gif")

        /** 先頭バイト（magic number）から対応画像を判定する。未対応・判定不能なら null。 */
        private fun detectImageType(bytes: ByteArray): ImageType? =
            when {
                startsWith(bytes, 0xFF, 0xD8, 0xFF) -> ImageType("image/jpeg", "jpg")
                startsWith(bytes, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A) ->
                    ImageType("image/png", "png")
                startsWith(bytes, 0x47, 0x49, 0x46, 0x38) -> ImageType("image/gif", "gif")
                isWebp(bytes) -> ImageType("image/webp", "webp")
                else -> null
            }

        private fun startsWith(
            bytes: ByteArray,
            vararg prefix: Int,
        ): Boolean {
            if (bytes.size < prefix.size) return false
            return prefix.withIndex().all { (i, b) -> bytes[i] == b.toByte() }
        }

        /** RIFF????WEBP（4-7 バイトはファイルサイズ）。 */
        private fun isWebp(bytes: ByteArray): Boolean =
            startsWith(bytes, 0x52, 0x49, 0x46, 0x46) &&
                bytes.size >= 12 &&
                bytes[8] == 0x57.toByte() &&
                bytes[9] == 0x45.toByte() &&
                bytes[10] == 0x42.toByte() &&
                bytes[11] == 0x50.toByte()
    }
}

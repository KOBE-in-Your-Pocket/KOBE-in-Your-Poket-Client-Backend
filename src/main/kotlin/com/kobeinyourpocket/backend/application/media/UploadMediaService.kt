package com.kobeinyourpocket.backend.application.media

import org.springframework.stereotype.Service
import java.util.UUID

/**
 * 画像アップロードユースケース（write）。
 *
 * 受け取ったファイルを content-type / サイズで検証し、サーバー側でキーを採番して
 * [MediaStorage] port へ保存し、公開 URL を返す。不正入力は [IllegalArgumentException]
 * （REST では 400）。domain 集約に属さない技術ユースケースのため application に置く。
 */
@Service
class UploadMediaService(
    private val mediaStorage: MediaStorage,
) {
    /**
     * @param bytes ファイルのバイト列
     * @param contentType リクエストの content-type（例: image/jpeg）
     */
    fun upload(
        bytes: ByteArray,
        contentType: String?,
    ): String {
        val normalizedType = contentType?.substringBefore(';')?.trim()?.lowercase()
        require(normalizedType != null && normalizedType in EXTENSION_BY_TYPE) {
            "unsupported content type: ${contentType ?: "(none)"}"
        }
        require(bytes.isNotEmpty()) { "empty file" }
        require(bytes.size <= MAX_BYTES) { "file too large" }

        val extension = EXTENSION_BY_TYPE.getValue(normalizedType)
        val key = "$KEY_PREFIX/${UUID.randomUUID()}.$extension"
        return mediaStorage.store(key = key, bytes = bytes, contentType = normalizedType)
    }

    companion object {
        const val MAX_BYTES: Int = 5 * 1024 * 1024
        const val KEY_PREFIX: String = "uploads"

        /** 許可する content-type と拡張子。 */
        private val EXTENSION_BY_TYPE =
            mapOf(
                "image/jpeg" to "jpg",
                "image/png" to "png",
                "image/webp" to "webp",
                "image/gif" to "gif",
            )
    }
}

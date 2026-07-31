package com.kobeinyourpocket.backend.application.media

/**
 * メディア（画像等）を外部ストレージへ保存する port。
 *
 * 実装は infrastructure（S3 アダプタ）。application は本 interface のみに依存する（§2）。
 */
interface MediaStorage {
    /**
     * バイト列を保存し、公開 URL を返す。
     *
     * @param key ストレージ上のオブジェクトキー（例: uploads/{uuid}.jpg）
     * @param bytes 保存するバイト列
     * @param contentType MIME タイプ（例: image/jpeg）
     */
    fun store(
        key: String,
        bytes: ByteArray,
        contentType: String,
    ): String
}

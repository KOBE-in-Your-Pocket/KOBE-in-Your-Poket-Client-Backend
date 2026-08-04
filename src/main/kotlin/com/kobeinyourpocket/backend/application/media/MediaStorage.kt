package com.kobeinyourpocket.backend.application.media

/**
 * メディア（画像等）を外部ストレージへ保存する port。
 *
 * 実装は infrastructure（S3 アダプタ）。application は本 interface のみに依存する（§2）。
 *
 * 保存したメディアは **staging（未確定）** と **確定済み** の 2 状態を持つ。
 * アップロードしただけで登録されなかった画像を溜めないため、staging は一定期間で
 * ストレージ側が自動削除する。エンティティに紐付いた時点で [commit] する。
 */
interface MediaStorage {
    /**
     * バイト列を **staging（未確定）** として保存し、公開 URL を返す。
     *
     * 保存直後から URL は有効だが、[commit] されなければ期限切れで削除される。
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

    /**
     * staging のメディアを **確定** し、自動削除の対象から外す。冪等。
     *
     * @param imageUrl [store] が返した公開 URL
     * @return 確定したら true。自ストレージ配下でない URL（外部 URL・シードデータ等）は
     *   何もせず false
     */
    fun commit(imageUrl: String): Boolean

    /**
     * 確定を取り消して **staging へ戻す**（自動削除の対象に差し戻す）。冪等。
     *
     * [commit] の後にエンティティの永続化が失敗すると、確定済みのまま誰からも参照されない
     * メディアが残るため、その巻き戻しに使う。
     *
     * @param imageUrl [store] が返した公開 URL
     * @return 差し戻したら true。自ストレージ配下でない URL は何もせず false
     */
    fun release(imageUrl: String): Boolean
}

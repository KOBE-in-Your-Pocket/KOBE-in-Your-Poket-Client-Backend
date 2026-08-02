package com.kobeinyourpocket.backend.infrastructure.storage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * 公開 URL → オブジェクトキーの解決（[S3MediaStorage.keyOf]）の検証。
 *
 * imageUrl はリクエストボディ由来なので、確定・差し戻しの対象を自分が発行した URL に
 * 限定できていることを確かめる。S3Client は生成されない（keyOf は遅延生成に触れない）。
 */
class S3MediaStorageTest {
    private val bucket = "kobe-media"
    private val region = "ap-northeast-1"

    private fun storage(publicBaseUrl: String = "") =
        S3MediaStorage(
            MediaStorageProperties(bucket = bucket, region = region, publicBaseUrl = publicBaseUrl),
        )

    @Test
    fun `S3 仮想ホスト形式の URL からキーを取り出す`() {
        val url = "https://$bucket.s3.$region.amazonaws.com/uploads/abc.jpg"

        assertEquals("uploads/abc.jpg", storage().keyOf(url))
    }

    @Test
    fun `publicBaseUrl 設定時は CloudFront の URL からキーを取り出す`() {
        val storage = storage(publicBaseUrl = "https://cdn.example.com")

        assertEquals("uploads/abc.jpg", storage.keyOf("https://cdn.example.com/uploads/abc.jpg"))
    }

    @Test
    fun `publicBaseUrl の末尾スラッシュは無視する`() {
        val storage = storage(publicBaseUrl = "https://cdn.example.com/")

        assertEquals("uploads/abc.jpg", storage.keyOf("https://cdn.example.com/uploads/abc.jpg"))
    }

    @Test
    fun `外部の URL は自ストレージ配下でないので null`() {
        // シードデータや手入力の URL。タグ操作の対象にしてはいけない。
        assertNull(storage().keyOf("https://example.com/photo.jpg"))
    }

    @Test
    fun `別バケットを装った URL は null`() {
        assertNull(storage().keyOf("https://other-bucket.s3.$region.amazonaws.com/uploads/abc.jpg"))
    }

    @Test
    fun `アップロード用プレフィクス外のキーは null`() {
        // バケット内の他用途オブジェクト（例: dev 画像ストア）へタグ操作させない。
        assertNull(storage().keyOf("https://$bucket.s3.$region.amazonaws.com/spots/seed.jpg"))
    }

    @Test
    fun `相対指定でプレフィクス制限を迂回する URL は null`() {
        assertNull(storage().keyOf("https://$bucket.s3.$region.amazonaws.com/uploads/../spots/seed.jpg"))
    }

    @Test
    fun `クエリやフラグメント付きの URL は null`() {
        assertNull(storage().keyOf("https://$bucket.s3.$region.amazonaws.com/uploads/abc.jpg?x=1"))
        assertNull(storage().keyOf("https://$bucket.s3.$region.amazonaws.com/uploads/abc.jpg#f"))
    }

    @Test
    fun `ベース URL そのものはプレフィクス外なので null`() {
        // キーが空文字列になり uploads/ 判定で外れる。
        assertNull(storage().keyOf("https://$bucket.s3.$region.amazonaws.com/"))
    }

    @Test
    fun `S3 未設定なら判定できないので null（S3 を呼ばない）`() {
        // ローカル / テスト環境。外部 URL でのスポット登録を例外で壊さない。
        val unconfigured = S3MediaStorage(MediaStorageProperties())

        assertNull(unconfigured.keyOf("https://$bucket.s3.$region.amazonaws.com/uploads/abc.jpg"))
    }
}

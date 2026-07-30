package com.kobeinyourpocket.backend.infrastructure.storage

import com.kobeinyourpocket.backend.application.media.MediaStorage
import org.springframework.beans.factory.DisposableBean
import org.springframework.stereotype.Component
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest

/**
 * [MediaStorage] の S3 実装（#86）。
 *
 * 認証情報は AWS SDK 既定の認証チェーン（IAM ロール / 環境変数）に委ね、コードに持たない。
 * bucket / region 未設定でも起動は妨げず、初回利用時に [IllegalStateException] で気付けるよう
 * S3Client は遅延生成し、Spring 終了時に（生成済みなら）close する。
 */
@Component
class S3MediaStorage(
    private val properties: MediaStorageProperties,
) : MediaStorage,
    DisposableBean {
    private val clientDelegate =
        lazy {
            check(properties.region.isNotBlank()) {
                "media.s3.region (MEDIA_S3_REGION) must be set to upload media"
            }
            S3Client.builder().region(Region.of(properties.region)).build()
        }
    private val client: S3Client by clientDelegate

    override fun store(
        key: String,
        bytes: ByteArray,
        contentType: String,
    ): String {
        check(properties.bucket.isNotBlank()) {
            "media.s3.bucket (MEDIA_S3_BUCKET) must be set to upload media"
        }
        val request =
            PutObjectRequest
                .builder()
                .bucket(properties.bucket)
                .key(key)
                .contentType(contentType)
                .build()
        client.putObject(request, RequestBody.fromBytes(bytes))
        return publicUrl(key)
    }

    /** Spring コンテキスト終了時に、生成済みのときだけ S3Client を close する（未使用なら初期化しない）。 */
    override fun destroy() {
        if (clientDelegate.isInitialized()) {
            clientDelegate.value.close()
        }
    }

    private fun publicUrl(key: String): String {
        val base = properties.publicBaseUrl.trim().trimEnd('/')
        if (base.isNotBlank()) return "$base/$key"
        return "https://${properties.bucket}.s3.${properties.region}.amazonaws.com/$key"
    }
}

package com.kobeinyourpocket.backend.infrastructure.storage

import com.kobeinyourpocket.backend.application.media.MediaStorage
import com.kobeinyourpocket.backend.application.media.command.UploadMediaService
import org.springframework.beans.factory.DisposableBean
import org.springframework.stereotype.Component
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectTaggingRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectTaggingRequest
import software.amazon.awssdk.services.s3.model.Tag
import software.amazon.awssdk.services.s3.model.Tagging
import java.time.Duration

/**
 * [MediaStorage] の S3 実装（#86）。
 *
 * 認証情報は AWS SDK 既定の認証チェーン（IAM ロール / 環境変数）に委ね、コードに持たない。
 * bucket / region 未設定でも起動は妨げず、初回利用時に [IllegalStateException] で気付けるよう
 * S3Client は遅延生成し、Spring 終了時に（生成済みなら）close する。
 * 同期 putObject が S3 遅延時にワーカースレッドを長時間占有しないよう API 呼び出しに上限を設ける。
 *
 * **未確定メディアの清理**: [store] は `status=staging` タグ付きで保存し、バケットの
 * ライフサイクル規則が回収する。[commit] はそのタグを外して回収対象から除外する。
 *
 * 対になるバケット側の設定（AWS CLI で適用済み・コード管理外）:
 * ```
 * ID     : expire-staging-media
 * Filter : Prefix "uploads/" AND Tag status=staging
 * Expire : 1 日（S3 の評価は 1 日 1 回 UTC 0 時なので削除は最大 2 日ほど遅れる）
 * IAM    : 実行ロールに s3:PutObject / s3:PutObjectTagging / s3:DeleteObjectTagging
 * ```
 * [STAGING_TAG_KEY] / [STAGING_TAG_VALUE] を変えると規則に一致しなくなり、未確定の画像が
 * 消えなくなる。変更する場合はバケットのライフサイクル規則も同時に更新すること。
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
            S3Client
                .builder()
                .region(Region.of(properties.region))
                .overrideConfiguration { override ->
                    override
                        .apiCallTimeout(Duration.ofSeconds(API_CALL_TIMEOUT_SECONDS))
                        .apiCallAttemptTimeout(Duration.ofSeconds(API_CALL_ATTEMPT_TIMEOUT_SECONDS))
                }.build()
        }
    private val client: S3Client by clientDelegate

    override fun store(
        key: String,
        bytes: ByteArray,
        contentType: String,
    ): String {
        requireBucket()
        val request =
            PutObjectRequest
                .builder()
                .bucket(properties.bucket)
                .key(key)
                .contentType(contentType)
                // 登録に使われなければライフサイクル規則で失効させる（確定は commit）。
                .tagging(stagingTagging())
                .build()
        client.putObject(request, RequestBody.fromBytes(bytes))
        return publicUrl(key)
    }

    override fun commit(imageUrl: String): Boolean {
        val key = keyOf(imageUrl) ?: return false
        // タグを消せばライフサイクル規則（タグ絞り込み）の対象から外れる。
        client.deleteObjectTagging(
            DeleteObjectTaggingRequest
                .builder()
                .bucket(properties.bucket)
                .key(key)
                .build(),
        )
        return true
    }

    override fun release(imageUrl: String): Boolean {
        val key = keyOf(imageUrl) ?: return false
        client.putObjectTagging(
            PutObjectTaggingRequest
                .builder()
                .bucket(properties.bucket)
                .key(key)
                .tagging(stagingTagging())
                .build(),
        )
        return true
    }

    /** Spring コンテキスト終了時に、生成済みのときだけ S3Client を close する（未使用なら初期化しない）。 */
    override fun destroy() {
        if (clientDelegate.isInitialized()) {
            clientDelegate.value.close()
        }
    }

    private fun requireBucket() {
        check(properties.bucket.isNotBlank()) {
            "media.s3.bucket (MEDIA_S3_BUCKET) must be set to upload media"
        }
    }

    private fun stagingTagging(): Tagging =
        Tagging
            .builder()
            .tagSet(
                Tag
                    .builder()
                    .key(STAGING_TAG_KEY)
                    .value(STAGING_TAG_VALUE)
                    .build(),
            ).build()

    private fun publicUrl(key: String): String = "${publicBaseUrl()}/$key"

    private fun publicBaseUrl(): String {
        val base = properties.publicBaseUrl.trim().trimEnd('/')
        if (base.isNotBlank()) return base
        return "https://${properties.bucket}.s3.${properties.region}.amazonaws.com"
    }

    /**
     * 公開 URL から自バケットのオブジェクトキーを取り出す。自分が発行した URL でなければ null。
     *
     * imageUrl はリクエストボディ由来なので、[publicBaseUrl] 配下かつアップロード用プレフィクス
     * 配下のキーだけを受け付ける（バケット内の任意オブジェクトへタグ操作されるのを防ぐ）。
     *
     * S3Client を触らないため、単体テストから直接呼べるよう internal にしている。
     */
    internal fun keyOf(imageUrl: String): String? {
        // S3 未設定の環境（ローカル / テスト）では自分の URL を判定できない。外部 URL と同じく
        // 対象外にして、S3 を使わないスポット登録（外部 URL 直指定）を壊さない。
        if (properties.publicBaseUrl.isBlank() &&
            (properties.bucket.isBlank() || properties.region.isBlank())
        ) {
            return null
        }
        val prefix = "${publicBaseUrl()}/"
        if (!imageUrl.startsWith(prefix)) return null
        val key = imageUrl.removePrefix(prefix)
        if (!key.startsWith("${UploadMediaService.KEY_PREFIX}/")) return null
        // "uploads/../secret" のような相対指定でプレフィクス制限を迂回されないようにする。
        if (key.contains("..") || key.contains('?') || key.contains('#')) return null
        return key
    }

    private companion object {
        // リトライ込みの全体上限と 1 試行あたりの上限。ハング時のスレッド占有を防ぐ。
        const val API_CALL_TIMEOUT_SECONDS = 30L
        const val API_CALL_ATTEMPT_TIMEOUT_SECONDS = 10L

        // バケットのライフサイクル規則が絞り込みに使うタグ。provision スクリプトと一致させる。
        const val STAGING_TAG_KEY = "status"
        const val STAGING_TAG_VALUE = "staging"
    }
}

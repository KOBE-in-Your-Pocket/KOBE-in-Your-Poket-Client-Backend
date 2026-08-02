package com.kobeinyourpocket.backend.infrastructure.storage

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 画像アップロード（S3）設定（#86）。
 *
 * [bucket] / [region] は必須（未設定時は [S3MediaStorage] 初回利用で IllegalStateException）。
 * [publicBaseUrl] は返却する公開 URL のベース（CloudFront 等）。空なら S3 仮想ホスト形式を使う。
 * 認証情報はコードに持たず、AWS SDK 既定の認証チェーン（IAM ロール / 環境変数）に委ねる。
 */
@ConfigurationProperties(prefix = "media.s3")
data class MediaStorageProperties(
    val bucket: String = "",
    val region: String = "",
    val publicBaseUrl: String = "",
)

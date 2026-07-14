package com.kobeinyourpocket.backend.infrastructure.security

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Supabase JWT 検証用の設定（#89）。
 *
 * 本番・ローカル（Signing Keys / ES256）は [SupabaseAuthProperties.url] 由来の JWKS で検証する。
 * [secret] はテスト（HS256）やレガシー JWT Secret 運用向けのフォールバック。
 */
@ConfigurationProperties(prefix = "supabase.jwt")
data class SupabaseJwtProperties(
    /** ダッシュボードの JWT Secret（HS256 用）。JWKS 運用では空でも可。 */
    val secret: String = "",
)

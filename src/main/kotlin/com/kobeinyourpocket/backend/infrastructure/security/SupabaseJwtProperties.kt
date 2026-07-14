package com.kobeinyourpocket.backend.infrastructure.security

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Supabase JWT 検証用の設定（#89）。
 *
 * [secret] はダッシュボード JWT Settings → JWT Secret（`SUPABASE_JWT_SECRET`）。
 * HS256 の共有秘密鍵として使う。JWKS への切替は別途 decoder を差し替える。
 */
@ConfigurationProperties(prefix = "supabase.jwt")
data class SupabaseJwtProperties(
    val secret: String,
)

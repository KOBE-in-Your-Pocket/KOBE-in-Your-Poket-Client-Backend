package com.kobeinyourpocket.backend.infrastructure.supabase

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Supabase Auth（GoTrue）呼び出し用（#89-b）。
 *
 * Client には渡さない。backend だけが [url] / [anonKey] を使う。
 * [url] / [anonKey] は必須。未設定の場合は [SupabaseAuthClient] 初回呼び出し時に
 * IllegalStateException で検知する。テスト環境では application-test.yml にダミー値を設定する。
 */
@ConfigurationProperties(prefix = "supabase")
data class SupabaseAuthProperties(
    val url: String = "",
    val anonKey: String = "",
    /** Supabase Admin API（GoTrue admin/users 操作）に必要な service_role キー。 */
    val serviceRoleKey: String = "",
)

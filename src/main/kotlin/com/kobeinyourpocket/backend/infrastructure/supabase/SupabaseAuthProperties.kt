package com.kobeinyourpocket.backend.infrastructure.supabase

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Supabase Auth（GoTrue）呼び出し用（#89-b）。
 *
 * Client には渡さない。backend だけが [url] / [anonKey] を使う。
 */
@ConfigurationProperties(prefix = "supabase")
data class SupabaseAuthProperties(
    val url: String,
    val anonKey: String,
)

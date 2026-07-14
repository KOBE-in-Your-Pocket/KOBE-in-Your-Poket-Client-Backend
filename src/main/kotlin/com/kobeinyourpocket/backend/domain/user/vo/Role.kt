package com.kobeinyourpocket.backend.domain.user.vo

/**
 * [値オブジェクト] ユーザーロール（一般 / 運営）。
 *
 * 正は Supabase JWT の `app_metadata` カスタムクレーム（#15）。
 * users プロフィールテーブルには永続化しない。JWT から解決して認可に使う。
 *
 * [claimValue] は `app_metadata.role` 等に載せる文字列と一致させる。
 */
enum class Role(
    val claimValue: String,
) {
    /** 一般ユーザー */
    GENERAL("general"),

    /** 運営 */
    OPERATOR("operator"),
    ;

    companion object {
        /** claim 文字列から解決する。未対応・空は `null`。 */
        fun of(value: String): Role? {
            val normalized = value.trim().lowercase()
            if (normalized.isEmpty()) return null
            return entries.firstOrNull { it.claimValue == normalized }
        }
    }
}

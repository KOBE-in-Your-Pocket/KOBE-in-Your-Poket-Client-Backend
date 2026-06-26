package com.kobeinyourpocket.backend.domain.tourism.vo

/**
 * ジャンル区分の Value Object（列挙）。
 *
 * Client `SpotGenre`（`domain/spot.ts`）と同値を API 契約として共有する。
 * [Companion.fromApiValue] が文字列 → enum への変換入口。
 */
enum class Genre(
    val apiValue: String,
) {
    LANDMARK("landmark"),
    NATURE("nature"),
    HISTORY("history"),
    GOURMET("gourmet"),
    ONSEN("onsen"),
    ;

    companion object {
        fun fromApiValue(value: String): Genre =
            entries.find { it.apiValue == value }
                ?: throw IllegalArgumentException("Unknown genre: $value")
    }
}

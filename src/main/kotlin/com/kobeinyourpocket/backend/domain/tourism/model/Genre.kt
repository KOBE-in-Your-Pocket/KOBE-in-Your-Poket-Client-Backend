package com.kobeinyourpocket.backend.domain.tourism.model

/**
 * 観光スポットのジャンル区分。
 *
 * Client `SpotGenre`（`domain/spot.ts`）と同値を API 契約として共有する。
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

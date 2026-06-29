package com.kobeinyourpocket.backend.features.tourism.domain.vo

/**
 * ジャンル区分の Value Object。
 *
 * ジャンルは運営（管理者）側で追加・拡張されうるため、固定列挙ではなく
 * 文字列をラップする汎用モデルとして定義する。
 * Client `SpotGenre`（`domain/spot.ts`）の既定値は [Companion] に定数として提供し、
 * 任意の値は [Companion.of] を入口に生成する。
 */
@JvmInline
value class Genre private constructor(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "Genre must not be blank" }
    }

    override fun toString(): String = value

    companion object {
        // Client `SpotGenre` 既知ジャンル（運営側で追加されうる）
        val LANDMARK = Genre("landmark")
        val NATURE = Genre("nature")
        val HISTORY = Genre("history")
        val GOURMET = Genre("gourmet")
        val ONSEN = Genre("onsen")

        fun of(value: String): Genre = Genre(value.trim())
    }
}

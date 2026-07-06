package com.kobeinyourpocket.backend.domain.evacuation.vo

/**
 * API / DB 文字列（wireValue）から enum を解決する共通ヘルパー。
 *
 * evacuation コンテキスト内の wire-mapped enum の [Companion.of] で利用する。
 */
internal inline fun <E> Iterable<E>.resolveByWireValue(
    value: String,
    crossinline wireValue: (E) -> String,
): E? {
    val normalized = value.trim().lowercase()
    return firstOrNull { wireValue(it) == normalized }
}

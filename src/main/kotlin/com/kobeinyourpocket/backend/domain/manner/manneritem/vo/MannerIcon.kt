package com.kobeinyourpocket.backend.domain.manner.manneritem.vo

/**
 * マナー項目のアイコン識別キー（値オブジェクト）。
 *
 * 画像 URL ではなくアイコンを一意に指すキー文字列（要件定義 M-3）。表示解決は Client 側に委ねる。
 * [Companion.of] が生成入口。
 */
@JvmInline
value class MannerIcon private constructor(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "MannerIcon must not be blank" }
    }

    override fun toString(): String = value

    companion object {
        fun of(value: String): MannerIcon = MannerIcon(value.trim())
    }
}

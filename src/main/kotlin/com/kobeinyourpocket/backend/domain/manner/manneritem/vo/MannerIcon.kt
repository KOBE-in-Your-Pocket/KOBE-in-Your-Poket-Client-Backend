package com.kobeinyourpocket.backend.domain.manner.manneritem.vo

/**
 * [値オブジェクト] マナー項目のアイコン識別キー。
 *
 * 画像 URL ではなくアイコンを一意に指すキー文字列（要件定義 M-3）。表示解決は Client 側に委ねる。
 * 運営側で追加されうるため固定列挙ではなく文字列ラップとし、[Companion.of] を生成入口とする。
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

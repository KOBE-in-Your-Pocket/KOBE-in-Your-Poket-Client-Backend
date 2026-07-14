package com.kobeinyourpocket.backend.domain.user.vo

/**
 * [値オブジェクト] ユーザーアイコンの画像 URL。
 *
 * 同一性ではなくプロフィール属性。未設定はエンティティ側で `null` とし、本型は「あり」のときだけ使う。
 * [Companion.of] が生成入口。
 */
@JvmInline
value class UserIcon private constructor(
    val url: String,
) {
    init {
        require(url.isNotBlank()) { "user icon url must not be blank" }
    }

    override fun toString(): String = url

    companion object {
        fun of(url: String): UserIcon = UserIcon(url.trim())
    }
}

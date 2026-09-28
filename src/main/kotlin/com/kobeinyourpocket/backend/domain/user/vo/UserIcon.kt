package com.kobeinyourpocket.backend.domain.user.vo

import java.net.URI

/**
 * [値オブジェクト] ユーザーアイコンの画像 URL。
 *
 * 同一性ではなくプロフィール属性。未設定はエンティティ側で `null` とし、本型は「あり」のときだけ使う。
 * [Companion.of] が生成入口。
 *
 * 値は **ホストを持つ絶対 http(s) URL** に限る（#184）。このアイコンはレビュー欄で他の利用者にも
 * 表示されるため、画像として読めない値や、画像以外を指す値を保存させない。
 * 実在確認はしない（保存時に読めても後から消えうるため、検証してもその状態は保てない）。
 */
@JvmInline
value class UserIcon private constructor(
    val url: String,
) {
    init {
        require(url.isNotBlank()) { "user icon url must not be blank" }
        require(url.length <= MAX_LENGTH) {
            "user icon url must be at most $MAX_LENGTH characters, got ${url.length}"
        }
        require(isHttpUrl(url)) { "user icon url must be an absolute http(s) URL with a host: '$url'" }
    }

    override fun toString(): String = url

    companion object {
        /** DB は TEXT だが、URL として現実的な上限で頭打ちにする（マナーアイコンの URL と同じ扱い）。 */
        const val MAX_LENGTH = 2048

        /** 受け付けるスキーム。Client が画像として読める形に限る。 */
        private val SCHEMES = setOf("http", "https")

        /**
         * スキームとホストを持つ絶対 URL か。
         *
         * 接頭辞だけを見ると `https://` のようなホスト無しの値を通してしまうため、
         * URI として解釈できることまで確かめる。
         */
        private fun isHttpUrl(value: String): Boolean =
            runCatching {
                val uri = URI(value)
                uri.scheme?.lowercase() in SCHEMES && !uri.host.isNullOrBlank()
            }.getOrDefault(false)

        fun of(url: String): UserIcon = UserIcon(url.trim())
    }
}

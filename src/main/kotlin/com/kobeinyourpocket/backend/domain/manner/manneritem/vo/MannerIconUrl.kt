package com.kobeinyourpocket.backend.domain.manner.manneritem.vo

/**
 * マナー項目のアイコン画像 URL（値オブジェクト）。
 *
 * 運営が管理画面からアップロードした画像の公開 URL。アイコン識別キー [MannerIcon] が
 * Client 同梱アセットへの参照だったのに対し、こちらは実体の場所を指す。
 * キーの追加には Client のリリースが要るため、運営だけでアイコンを増やせるようにする経路。
 *
 * 実在確認はしない。保存時点で到達できても後から消えうるため、検証してもその状態は保てない。
 * [Companion.of] が生成入口。
 */
@JvmInline
value class MannerIconUrl private constructor(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "MannerIconUrl must not be blank" }
        require(value.length <= MAX_LENGTH) {
            "MannerIconUrl must be at most $MAX_LENGTH characters, got ${value.length}"
        }
        require(SCHEMES.any { value.startsWith(it) }) {
            "MannerIconUrl must start with one of ${SCHEMES.joinToString()}: '$value'"
        }
    }

    override fun toString(): String = value

    companion object {
        /** DB は TEXT だが、URL として現実的な上限で頭打ちにする。 */
        const val MAX_LENGTH = 2048

        /** 受け付けるスキーム。Client が画像として読める形に限る。 */
        private val SCHEMES = listOf("https://", "http://")

        fun of(value: String): MannerIconUrl = MannerIconUrl(value.trim())
    }
}

package com.kobeinyourpocket.backend.domain.manner.manneritem.vo

/**
 * [値オブジェクト] マナー項目識別子。
 *
 * 集約 `model.MannerItem` の同一性（identity）を表す。[Companion.of] が生成入口。
 */
@JvmInline
value class MannerItemId private constructor(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "MannerItemId must not be blank" }
        require(value.length <= MAX_LENGTH) {
            "MannerItemId must be at most $MAX_LENGTH characters, got ${value.length}"
        }
    }

    override fun toString(): String = value

    companion object {
        private const val MAX_LENGTH = 128

        fun of(value: String): MannerItemId = MannerItemId(value.trim())
    }
}

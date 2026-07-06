package com.kobeinyourpocket.backend.domain.evacuation.vo

/**
 * 避難所識別子の Value Object（slug 等）。
 *
 * 集約 `aggregate.EvacuationShelter` の同一性（identity）を表す。[Companion.of] が生成入口。
 * Client `EvacuationShelter.id` に対応する。
 */
@JvmInline
value class ShelterId private constructor(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "ShelterId must not be blank" }
        require(value.length <= MAX_LENGTH) {
            "ShelterId must be at most $MAX_LENGTH characters, got ${value.length}"
        }
    }

    override fun toString(): String = value

    companion object {
        private const val MAX_LENGTH = 128

        fun of(value: String): ShelterId = ShelterId(value.trim())
    }
}

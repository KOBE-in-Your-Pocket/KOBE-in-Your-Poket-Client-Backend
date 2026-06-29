package com.kobeinyourpocket.backend.tourism.domain.vo

/**
 * スポット識別子の Value Object（slug 等）。
 *
 * 集約 `aggregate.Spot` の同一性（identity）を表す。[Companion.of] が生成入口。
 */
@JvmInline
value class SpotId private constructor(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "SpotId must not be blank" }
        require(value.length <= MAX_LENGTH) {
            "SpotId must be at most $MAX_LENGTH characters, got ${value.length}"
        }
    }

    override fun toString(): String = value

    companion object {
        private const val MAX_LENGTH = 128

        fun of(value: String): SpotId = SpotId(value.trim())
    }
}

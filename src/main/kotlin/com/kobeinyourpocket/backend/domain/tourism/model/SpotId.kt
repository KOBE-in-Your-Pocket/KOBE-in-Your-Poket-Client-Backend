package com.kobeinyourpocket.backend.domain.tourism.model

/**
 * 観光スポットの識別子（slug 等）。
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

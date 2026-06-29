package com.kobeinyourpocket.backend.domain.tourism.vo

import java.util.UUID

/**
 * レビュー識別子の Value Object（UUID）。
 *
 * [generate] がサーバー側採番の入口。既存 UUID からの復元は [of] を使う。
 */
@JvmInline
value class ReviewId private constructor(
    val value: UUID,
) {
    override fun toString(): String = value.toString()

    companion object {
        /** 新規レビュー投稿時にサーバー側で採番する。 */
        fun generate(): ReviewId = ReviewId(UUID.randomUUID())

        fun of(value: UUID): ReviewId = ReviewId(value)

        /**
         * UUID 文字列から復元する。不正な形式は [IllegalArgumentException] をスローする。
         */
        fun of(value: String): ReviewId =
            try {
                ReviewId(UUID.fromString(value))
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Invalid ReviewId format: $value", e)
            }
    }
}

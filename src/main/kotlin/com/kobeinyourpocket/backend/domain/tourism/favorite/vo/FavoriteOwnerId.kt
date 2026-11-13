package com.kobeinyourpocket.backend.domain.tourism.favorite.vo

import java.util.UUID

/**
 * [値オブジェクト] お気に入りした利用者の識別子（Supabase Auth の user id）。
 *
 * 実体は `domain.user` の `User.Id` と同じ UUID だが、tourism から user コンテキストの
 * モデルを直接参照しないよう、tourism 側の VO として持つ（[com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewAuthorId] と同じ）。
 */
@JvmInline
value class FavoriteOwnerId private constructor(
    val value: UUID,
) {
    override fun toString(): String = value.toString()

    companion object {
        fun of(value: UUID): FavoriteOwnerId = FavoriteOwnerId(value)

        /**
         * UUID 文字列（JWT の subject など）から復元する。不正な形式は [IllegalArgumentException] をスローする。
         */
        fun of(value: String): FavoriteOwnerId =
            try {
                FavoriteOwnerId(UUID.fromString(value))
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Invalid FavoriteOwnerId format: $value", e)
            }
    }
}

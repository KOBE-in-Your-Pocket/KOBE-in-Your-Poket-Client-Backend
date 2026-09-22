package com.kobeinyourpocket.backend.domain.tourism.review.vo

import java.util.UUID

/**
 * [値オブジェクト] レビュー投稿者の識別子（Supabase Auth の user id / #86）。
 *
 * 実体は `domain.user` の `User.Id` と同じ UUID だが、tourism から user コンテキストの
 * モデルを直接参照しないよう、tourism 側の VO として持つ。
 *
 * 表示名（[ReviewAuthor.name]）は一意でも不変でもないため本人判定には使えない。
 * 「誰の投稿か」を決めるのは常にこの id。
 */
@JvmInline
value class ReviewAuthorId private constructor(
    val value: UUID,
) {
    override fun toString(): String = value.toString()

    companion object {
        fun of(value: UUID): ReviewAuthorId = ReviewAuthorId(value)

        /**
         * UUID 文字列から復元する。不正な形式は [IllegalArgumentException] をスローする。
         */
        fun of(value: String): ReviewAuthorId =
            try {
                ReviewAuthorId(UUID.fromString(value))
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Invalid ReviewAuthorId format: $value", e)
            }
    }
}

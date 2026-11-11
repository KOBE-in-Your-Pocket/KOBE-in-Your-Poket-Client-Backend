package com.kobeinyourpocket.backend.infrastructure.rest.tourism

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

/**
 * `POST /api/v1/tourism/spots/{spotId}/reviews` のリクエストボディ（§8）。
 *
 * 投稿者の表示名・アイコンは JWT の `sub` に紐づくプロフィールから決める（#93 / #210）。
 * [author] は旧 Client との互換のために受け付けるだけで、値は使わない（送らなくてよい）。
 */
data class ReviewRequest(
    @field:Min(1)
    @field:Max(5)
    val rating: Int,
    @field:NotBlank
    val comment: String,
    val author: AuthorBody? = null,
    @field:NotBlank
    val language: String,
) {
    /** 互換用。中身は検証も保存もしない。 */
    data class AuthorBody(
        val name: String? = null,
        val iconUrl: String? = null,
    )
}

package com.kobeinyourpocket.backend.application.tourism.query

import java.time.Instant

/**
 * 言語解決済みレビューの読みモデル。
 *
 * CQRS read 側専用。command 側の集約 [com.kobeinyourpocket.backend.domain.tourism.review.model.Review] とは別経路。
 */
data class ReviewView(
    val id: String,
    val spotId: String,
    val rating: Int,
    val comment: String,
    val authorName: String,
    val authorIconUrl: String?,
    val createdAt: Instant,
    val language: String,
)

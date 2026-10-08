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
    /** 投稿者の user id。V17 以前の投稿は特定できないため null（#86）。 */
    val authorUserId: String?,
    val createdAt: Instant,
    val language: String,
    /**
     * 運営が通報を承認し、非表示に同意した（不適切と判断した）口コミか。true なら Client はアプリ全体で非表示にする。
     * 通報が却下された・まだ確認されていないだけの口コミは false。
     */
    val hiddenByReport: Boolean,
)

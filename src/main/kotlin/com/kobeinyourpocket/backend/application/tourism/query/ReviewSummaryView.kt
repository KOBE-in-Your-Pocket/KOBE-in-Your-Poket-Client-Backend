package com.kobeinyourpocket.backend.application.tourism.query

import java.time.Instant

/**
 * 運営向けレビュー横断一覧の 1 件（read / #165）。
 *
 * スポット別の [ReviewView] と違い、どのスポットへのレビューかを持つ。管理画面は
 * 全スポットを横断して並べ、スポット名で絞り込むため。
 *
 * [spotName] は要求言語で解決済み（en フォールバック）。一方 [comment] / [authorName] は
 * **投稿時の言語のまま**で、[language] がその言語を表す。運営はスポット名を自分の言語で読みつつ、
 * レビュー本文は原文で確認する必要があるため解決しない。
 */
data class ReviewSummaryView(
    val id: String,
    val spotId: String,
    val spotName: String,
    val rating: Int,
    val comment: String,
    val authorName: String,
    val authorIconUrl: String?,
    val createdAt: Instant,
    val language: String,
)

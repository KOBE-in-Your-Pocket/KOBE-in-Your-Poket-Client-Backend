package com.kobeinyourpocket.backend.application.stats.query

import java.time.Instant

/**
 * 管理画面ダッシュボードが 1 画面で必要とする集計結果（read / #169）。
 *
 * ADMIN は統計 API が無い間、一覧 API を取得してフロントで集計していた。
 * 一覧 API は 1 リクエスト 200 件が上限のため、総数以外は部分集計にしかならない。
 * 本 View は COUNT / GROUP BY をサーバー側で行い、その制約を無くすためのもの。
 */
data class DashboardStatsView(
    /** 全期間の総数。 */
    val totals: EntityCountsView,
    /** 今月（Asia/Tokyo の月初〜翌月初）に登録された件数。 */
    val thisMonth: EntityCountsView,
    /** 先月に登録された件数。前月比の分母に使う。 */
    val lastMonth: EntityCountsView,
    /** レビュー数の多い順のスポット。 */
    val popularSpots: List<PopularSpotView>,
    /** 投稿の新しい順のレビュー。 */
    val recentReviews: List<RecentReviewView>,
)

/** ユーザー・スポット・レビューの件数。総数と期間集計で同じ形を使う。 */
data class EntityCountsView(
    val users: Long,
    val spots: Long,
    val reviews: Long,
)

/** 人気スポット 1 件。[name] は要求言語で解決済み。 */
data class PopularSpotView(
    val spotId: String,
    val name: String,
    val reviewCount: Long,
)

/**
 * 直近のレビュー 1 件。
 *
 * [spotName] だけ要求言語で解決する。コメント本文は載せず、投稿者名は投稿時の言語のまま
 * （横断一覧 `GET /api/v1/tourism/reviews` と同じ方針）。ダッシュボードは「いつ・誰が・
 * どのスポットに」が分かれば十分で、本文が要るなら一覧画面へ遷移させる。
 */
data class RecentReviewView(
    val id: String,
    val spotId: String,
    val spotName: String,
    val authorName: String,
    val rating: Int,
    val postedAt: Instant,
    val language: String,
)

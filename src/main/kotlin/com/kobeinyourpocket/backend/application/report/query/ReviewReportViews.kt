package com.kobeinyourpocket.backend.application.report.query

import java.time.Instant

/**
 * 運営向け通報一覧の 1 ページ分（read / #145）。口コミ単位でまとめた [groups] を持つ。
 *
 * 総件数（＝通報された口コミの数）を持つのは、管理画面のページャが「全 N 件中」を描くのに必要なため。
 */
data class ReviewReportPageView(
    val groups: List<ReviewReportGroupView>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
) {
    /** 総ページ数。[size] が 0 以下になる経路は [ListReviewReportsService] が塞ぐ。 */
    val totalPages: Int
        get() = if (size <= 0) 0 else ((totalElements + size - 1) / size).toInt()
}

/**
 * 通報された口コミ 1 件分。件数・内訳・明細は、一覧の絞り込みにかかわらず**その口コミの全通報**から作る。
 *
 * [review] は口コミが削除済みなら null（通報は対応履歴として残る）。
 */
data class ReviewReportGroupView(
    val reviewId: String,
    val review: ReportedReviewView?,
    val reportCount: Int,
    val openCount: Int,
    /** 理由の code ごとの件数。通報の無い理由は含めない。 */
    val reasonCounts: Map<String, Int>,
    val latestReportedAt: Instant,
    /** 新しい順。 */
    val reports: List<ReviewReportItemView>,
)

/**
 * 通報された口コミの中身。運営が一覧だけで判断できるよう本文・投稿者・スポット名を持つ。
 *
 * [spotName] は要求言語で解決済み。[comment] / [authorName] は投稿時の言語のまま（[language]）。
 */
data class ReportedReviewView(
    val spotId: String,
    val spotName: String,
    val rating: Int,
    val comment: String,
    /** V17 以前の投稿は投稿者を特定できず null。 */
    val authorUserId: String?,
    val authorName: String,
    val language: String,
    val postedAt: Instant,
)

/** 通報 1 件分。 */
data class ReviewReportItemView(
    val id: String,
    val reason: String,
    val description: String?,
    val status: String,
    val reporterUserId: String,
    /** 通報者の表示名。プロフィール行が無ければ null。 */
    val reporterName: String?,
    val createdAt: Instant,
    val handledBy: String?,
    val handledAt: Instant?,
)

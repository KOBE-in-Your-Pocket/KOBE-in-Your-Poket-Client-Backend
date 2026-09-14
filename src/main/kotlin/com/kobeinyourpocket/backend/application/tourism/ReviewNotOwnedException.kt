package com.kobeinyourpocket.backend.application.tourism

import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewId

/**
 * 他人のレビューを本人向け操作（編集・削除 / #86）で変更しようとした場合の例外（REST では 403）。
 *
 * 404 ではなく 403 にするのは、レビュー自体は一覧で公開されており存在が秘密ではないため。
 * 存在しない ID は [ReviewNotFoundException]（404）で先に弾かれる。
 *
 * V17 以前の投稿者不明（`author_user_id` が NULL）のレビューもここに落ちる。
 * 本人を確定できない以上、誰にも本人操作を許さないのが安全側の倒し方。
 */
class ReviewNotOwnedException(
    id: ReviewId,
) : RuntimeException("Review is not owned by the requester: ${id.value}")

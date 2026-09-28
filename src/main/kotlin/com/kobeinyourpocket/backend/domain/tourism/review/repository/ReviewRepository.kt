package com.kobeinyourpocket.backend.domain.tourism.review.repository

import com.kobeinyourpocket.backend.domain.tourism.review.model.Review
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewAuthorId
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewId

/** [リポジトリ] write port（command）。read は application.tourism.query.ReviewQuery。 */
interface ReviewRepository {
    fun save(review: Review): Review

    fun findById(id: ReviewId): Review?

    fun existsById(id: ReviewId): Boolean

    /** レビューを削除する。集約に子は無いため単独で消える（#165）。 */
    fun deleteById(id: ReviewId)

    /**
     * 指定した投稿者のレビューを全件削除する。退会処理から呼ぶ（#528）。
     *
     * V17 以前に投稿されたレビューは `author_user_id` が NULL で投稿者を特定できないため
     * 対象にならない。該当が 0 件でも例外にはしない。
     *
     * スポットの平均評価は読み取り時に `AVG(rating)` で算出しているため、
     * 削除後の再集計は不要（SpotQueryJpa）。
     *
     * @return 削除した件数
     */
    fun deleteByAuthorId(authorId: ReviewAuthorId): Int
}

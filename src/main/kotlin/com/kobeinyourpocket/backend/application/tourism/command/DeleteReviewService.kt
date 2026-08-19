package com.kobeinyourpocket.backend.application.tourism.command

import com.kobeinyourpocket.backend.application.tourism.ReviewNotFoundException
import com.kobeinyourpocket.backend.domain.tourism.review.repository.ReviewRepository
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewId
import org.springframework.stereotype.Service

/**
 * 運営によるレビュー削除ユースケース（モデレーション / 要件表 C-9・#165）。
 * 運営ロール限定で、REST 側は `@PreAuthorize` で担保する。
 *
 * **投稿者本人による削除（#86 / R-2）とは別**。本ユースケースは他人のレビューを消すことが
 * 前提で、本人判定を行わない。#86 の本人削除を実装するときは、本人判定を伴う別経路になる。
 *
 * レビュー集約に子は無いため、関連の連鎖削除は無い。スポットごと削除した場合は
 * `review` が `ON DELETE CASCADE`（V2）で連動して消える。
 */
@Service
class DeleteReviewService(
    private val reviewRepository: ReviewRepository,
) {
    fun execute(id: ReviewId) {
        if (!reviewRepository.existsById(id)) throw ReviewNotFoundException(id)
        reviewRepository.deleteById(id)
    }
}

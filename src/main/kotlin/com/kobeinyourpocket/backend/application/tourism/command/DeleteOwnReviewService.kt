package com.kobeinyourpocket.backend.application.tourism.command

import com.kobeinyourpocket.backend.application.tourism.ReviewNotFoundException
import com.kobeinyourpocket.backend.application.tourism.ReviewNotOwnedException
import com.kobeinyourpocket.backend.domain.tourism.review.repository.ReviewRepository
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewAuthorId
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewId
import org.springframework.stereotype.Service

/**
 * 投稿者本人によるレビュー削除ユースケース（#86）。
 *
 * 運営のモデレーション削除（[DeleteReviewService] / #165）とは経路を分けている。
 * あちらは他人の投稿を消すことが前提で本人判定を行わないため、同じサービスに
 * 相乗りさせると「本人判定を忘れた呼び出し」を型で防げなくなる。
 *
 * 投稿者を特定できない V17 以前のレビューは [ReviewNotOwnedException] になる。
 * 消したいユーザーには運営問い合わせで対応する（モデレーション削除で消せる）。
 */
@Service
class DeleteOwnReviewService(
    private val reviewRepository: ReviewRepository,
) {
    fun execute(
        id: ReviewId,
        requesterId: ReviewAuthorId,
    ) {
        val existing = reviewRepository.findById(id) ?: throw ReviewNotFoundException(id)
        if (!existing.author.isOwnedBy(requesterId)) throw ReviewNotOwnedException(id)
        reviewRepository.deleteById(id)
    }
}

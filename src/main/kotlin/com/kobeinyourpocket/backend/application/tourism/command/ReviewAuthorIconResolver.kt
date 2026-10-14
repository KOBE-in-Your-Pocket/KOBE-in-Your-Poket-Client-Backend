package com.kobeinyourpocket.backend.application.tourism.command

import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewAuthorId
import com.kobeinyourpocket.backend.domain.user.model.User
import com.kobeinyourpocket.backend.domain.user.repository.UserRepository
import org.springframework.stereotype.Component

/**
 * 投稿・編集レスポンスに載せる投稿者アイコンを、投稿者の現在のプロフィールから解決する（#208 / #210）。
 *
 * 一覧（`ReviewQueryJpa` の `AUTHOR_ICON_URL`）と同じ規則にそろえ、投稿・編集の直後と
 * 再取得後で表示が食い違わないようにする。
 * - プロフィール行がある → そのアイコン（未設定なら null。スナップショットへは戻さない）
 * - プロフィール行が無い → [snapshot]（レビューに保存済みの値）
 *
 * tourism のドメインから user のモデルを参照しないよう、コンテキストをまたぐ読み取りは
 * application 層のここに閉じ込める。
 */
@Component
class ReviewAuthorIconResolver(
    private val userRepository: UserRepository,
) {
    fun resolve(
        authorUserId: ReviewAuthorId,
        snapshot: String?,
    ): String? {
        val profile = userRepository.findById(User.Id.of(authorUserId.value)) ?: return snapshot
        return profile.icon?.url
    }
}

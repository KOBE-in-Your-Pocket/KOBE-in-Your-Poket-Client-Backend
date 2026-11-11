package com.kobeinyourpocket.backend.application.tourism.command

import com.kobeinyourpocket.backend.application.user.command.UserNotFoundException
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewAuthor
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewAuthorId
import com.kobeinyourpocket.backend.domain.user.model.User
import com.kobeinyourpocket.backend.domain.user.repository.UserRepository
import org.springframework.stereotype.Component

/**
 * レビューの投稿者（表示名・アイコン）を、投稿者の現在のプロフィールから解決する（#93 / #208 / #210）。
 *
 * 表示名もアイコンもリクエストの自己申告は使わない。JWT の `sub`（[ReviewAuthorId]）に紐づく
 * プロフィールだけを根拠にし、他人の名前をかたった投稿を防ぐ。
 *
 * 一覧（`ReviewQueryJpa` の `AUTHOR_NAME` / `AUTHOR_ICON_URL`）と同じ規則にそろえ、投稿・編集の直後と
 * 再取得後で表示が食い違わないようにする。レビュー行に残す値はプロフィール行が無い場合の控え。
 *
 * tourism のドメインから user のモデルを参照しないよう、コンテキストをまたぐ読み取りは
 * application 層のここに閉じ込める。
 */
@Component
class ReviewAuthorResolver(
    private val userRepository: UserRepository,
) {
    /**
     * 新規投稿の投稿者を作る。
     *
     * プロフィール行はサインイン時に必ず作られるため、無いのは異常系として [UserNotFoundException]（404）。
     * リクエストの名前に戻すと、その経路だけ名前をかたれてしまうため戻さない。
     */
    fun forNewReview(authorUserId: ReviewAuthorId): ReviewAuthor {
        val profile = findProfile(authorUserId) ?: throw UserNotFoundException(User.Id.of(authorUserId.value))
        return ReviewAuthor(name = profile.name, iconUrl = profile.icon?.url, userId = authorUserId)
    }

    /**
     * 編集時に投稿者を現在のプロフィールで更新する。
     * - プロフィール行がある → その表示名・アイコン（アイコン未設定なら null。控えへは戻さない）
     * - プロフィール行が無い → [current]（レビューに保存済みの控え）のまま
     */
    fun refresh(
        current: ReviewAuthor,
        authorUserId: ReviewAuthorId,
    ): ReviewAuthor {
        val profile = findProfile(authorUserId) ?: return current
        return current.copy(name = profile.name, iconUrl = profile.icon?.url)
    }

    private fun findProfile(authorUserId: ReviewAuthorId): User? = userRepository.findById(User.Id.of(authorUserId.value))
}

package com.kobeinyourpocket.backend.application.user.command

import com.kobeinyourpocket.backend.application.user.auth.AuthGateway
import com.kobeinyourpocket.backend.domain.tourism.review.repository.ReviewRepository
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewAuthorId
import com.kobeinyourpocket.backend.domain.user.model.User
import com.kobeinyourpocket.backend.domain.user.repository.UserRepository
import org.springframework.stereotype.Service

/**
 * ユーザーを完全削除するユースケース。
 *
 * 呼び出し口は 2 つあり、どちらも同じ削除を行う。
 * - 運営による削除: `DELETE /api/v1/auth/users/{userId}`（ADMIN 限定）
 * - 本人による退会: `DELETE /api/v1/users/me`（対象は JWT の subject。#528）
 *
 * 「誰を消してよいか」は呼び出し側（認可と subject の解決）の責務で、ここでは判定しない。
 *
 * 外部 HTTP（Supabase Admin API）は DB トランザクション外で実行する。
 * `@Transactional` で包むと Auth 削除成功後に `deleteById` が失敗・ロールバックしたとき、
 * Auth だけ消えプロフィール行が残る不整合になるためである。
 *
 * 削除順序:
 * 1. プロフィール行の存在確認（DB・読み取り）
 * 2. Supabase Auth からユーザーを削除（Admin API。未存在=404 は冪等成功）
 * 3. 本人が投稿したレビューを削除（#528。DB 書き込み）
 * 4. プロフィール行を削除（短い DB 書き込み。リポジトリ側のトランザクション）
 *
 * Auth 成功後に DB 側だけ失敗した場合は再試行する。再試行時は Auth 側 404 を成功扱いし、
 * 残ったレビューとプロフィール行の削除を完了できる。レビュー削除は該当 0 件でも成功するため、
 * 途中まで進んだ状態からの再実行でも同じ結果になる。
 *
 * レビューを Auth 削除より後に消すのは、Auth 削除が失敗したとき（＝退会が成立していないとき）に
 * レビューだけ消えるのを避けるためである。
 */
@Service
class DeleteUserService(
    private val authGateway: AuthGateway,
    private val userRepository: UserRepository,
    private val reviewRepository: ReviewRepository,
) {
    fun execute(userId: User.Id) {
        userRepository.findById(userId) ?: throw UserNotFoundException(userId)
        authGateway.deleteUser(userId)
        reviewRepository.deleteByAuthorId(ReviewAuthorId.of(userId.value))
        userRepository.deleteById(userId)
    }
}

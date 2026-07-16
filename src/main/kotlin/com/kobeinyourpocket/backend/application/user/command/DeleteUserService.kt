package com.kobeinyourpocket.backend.application.user.command

import com.kobeinyourpocket.backend.application.user.auth.AuthGateway
import com.kobeinyourpocket.backend.domain.user.model.User
import com.kobeinyourpocket.backend.domain.user.repository.UserRepository
import org.springframework.stereotype.Service

/**
 * admin ロール専用のユーザー完全削除ユースケース。
 *
 * 外部 HTTP（Supabase Admin API）は DB トランザクション外で実行する。
 * `@Transactional` で包むと Auth 削除成功後に `deleteById` が失敗・ロールバックしたとき、
 * Auth だけ消えプロフィール行が残る不整合になるためである。
 *
 * 削除順序:
 * 1. プロフィール行の存在確認（DB・読み取り）
 * 2. Supabase Auth からユーザーを削除（Admin API。未存在=404 は冪等成功）
 * 3. プロフィール行を削除（短い DB 書き込み。リポジトリ側のトランザクション）
 *
 * Auth 成功後に DB 削除だけ失敗した場合は再試行する。再試行時は Auth 側 404 を成功扱いし、
 * 残ったプロフィール行の削除を完了できる。
 */
@Service
class DeleteUserService(
    private val authGateway: AuthGateway,
    private val userRepository: UserRepository,
) {
    fun execute(userId: User.Id) {
        userRepository.findById(userId) ?: throw UserNotFoundException(userId)
        authGateway.deleteUser(userId)
        userRepository.deleteById(userId)
    }
}

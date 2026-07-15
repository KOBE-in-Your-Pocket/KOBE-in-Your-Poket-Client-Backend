package com.kobeinyourpocket.backend.application.user.command

import com.kobeinyourpocket.backend.application.user.auth.AuthGateway
import com.kobeinyourpocket.backend.domain.user.model.User
import com.kobeinyourpocket.backend.domain.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * admin ロール専用のユーザー完全削除ユースケース。
 *
 * 削除順序:
 * 1. プロフィール行の存在確認（DB）
 * 2. Supabase Auth からユーザーを削除（Admin API / service_role キー）
 * 3. プロフィール行を削除（DB）
 *
 * Supabase 削除成功後に DB 削除が失敗した場合、Auth ユーザーは既に存在しないため
 * ログインは不可能になる。運用上は再試行または手動での DB クリーンアップで対処する。
 */
@Service
class DeleteUserService(
    private val authGateway: AuthGateway,
    private val userRepository: UserRepository,
) {
    @Transactional
    fun execute(userId: User.Id) {
        userRepository.findById(userId) ?: throw UserNotFoundException(userId)
        authGateway.deleteUser(userId)
        userRepository.deleteById(userId)
    }
}

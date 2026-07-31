package com.kobeinyourpocket.backend.application.user.query

import com.kobeinyourpocket.backend.application.user.command.UserNotFoundException
import com.kobeinyourpocket.backend.domain.user.model.PublicUser
import com.kobeinyourpocket.backend.domain.user.model.User
import com.kobeinyourpocket.backend.domain.user.repository.UserRepository
import org.springframework.stereotype.Service

/**
 * 認証ユーザー自身の公開プロフィール取得ユースケース（read）。
 *
 * プロフィール行が無ければ [UserNotFoundException]（ログイン時に自動作成されるため、
 * 発生するのはトークン有効中に削除された場合など）。
 */
@Service
class GetMeService(
    private val userRepository: UserRepository,
) {
    fun execute(userId: User.Id): PublicUser = userRepository.findById(userId)?.toPublicUser() ?: throw UserNotFoundException(userId)
}

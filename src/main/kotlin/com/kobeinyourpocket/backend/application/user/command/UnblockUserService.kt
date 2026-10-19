package com.kobeinyourpocket.backend.application.user.command

import com.kobeinyourpocket.backend.domain.user.model.User
import com.kobeinyourpocket.backend.domain.user.repository.UserBlockRepository
import org.springframework.stereotype.Service

/**
 * ブロックを解除するユースケース（#215）。
 *
 * ブロックしていない相手・退会済みの相手を指定しても成功として扱う（冪等）。
 * 解除できないケースを作ると、退会済みユーザーが一覧に残ったまま消せなくなるため。
 */
@Service
class UnblockUserService(
    private val userBlockRepository: UserBlockRepository,
) {
    fun execute(
        blockerId: User.Id,
        blockedId: User.Id,
    ) {
        userBlockRepository.delete(blockerId = blockerId, blockedId = blockedId)
    }
}

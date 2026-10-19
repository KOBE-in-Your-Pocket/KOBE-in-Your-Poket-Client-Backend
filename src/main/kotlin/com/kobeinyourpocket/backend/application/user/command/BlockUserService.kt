package com.kobeinyourpocket.backend.application.user.command

import com.kobeinyourpocket.backend.domain.user.model.User
import com.kobeinyourpocket.backend.domain.user.model.UserBlock
import com.kobeinyourpocket.backend.domain.user.repository.UserBlockRepository
import com.kobeinyourpocket.backend.domain.user.repository.UserRepository
import org.springframework.stereotype.Service

/**
 * ほかの利用者をブロックするユースケース（#215）。App Store Guideline 1.2 のレビュー解禁条件。
 *
 * ブロックすると、ブロックした側のレビュー一覧からブロックされた側のレビューが消える
 * （[com.kobeinyourpocket.backend.application.tourism.query.ListReviewsService]）。
 * 記録はサーバーに残るため、運営はどのユーザーがブロックされているかを把握できる。
 *
 * 判定順:
 * 1. 自分自身でないこと（自分なら 400。[UserBlock] の不変条件）
 * 2. 相手のプロフィールが存在すること（無ければ 404）
 *
 * すでにブロック済みでも成功として扱う（冪等）。[blockerId] は REST 層が JWT の `sub` から渡す。
 */
@Service
class BlockUserService(
    private val userRepository: UserRepository,
    private val userBlockRepository: UserBlockRepository,
) {
    fun execute(
        blockerId: User.Id,
        blockedId: User.Id,
    ) {
        val block = UserBlock.create(blockerId = blockerId, blockedId = blockedId)
        userRepository.findById(blockedId) ?: throw UserNotFoundException(blockedId)
        userBlockRepository.saveIfAbsent(block)
    }
}

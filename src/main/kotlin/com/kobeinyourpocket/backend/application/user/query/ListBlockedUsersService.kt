package com.kobeinyourpocket.backend.application.user.query

import com.kobeinyourpocket.backend.domain.user.model.User
import org.springframework.stereotype.Service

/** ブロック中のユーザー一覧取得ユースケース（read / #215）。[UserBlockQuery] port へ委譲する。 */
@Service
class ListBlockedUsersService(
    private val userBlockQuery: UserBlockQuery,
) {
    fun execute(blockerId: User.Id): List<BlockedUserView> = userBlockQuery.findBlockedUsers(blockerId)
}

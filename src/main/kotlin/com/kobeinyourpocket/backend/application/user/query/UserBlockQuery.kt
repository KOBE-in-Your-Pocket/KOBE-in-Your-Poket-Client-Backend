package com.kobeinyourpocket.backend.application.user.query

import com.kobeinyourpocket.backend.domain.user.model.User

/** read 専用 port。application が定義し infrastructure.query が実装する。 */
interface UserBlockQuery {
    /** [blockerId] がブロック中のユーザーを、ブロックした日時の新しい順に返す。 */
    fun findBlockedUsers(blockerId: User.Id): List<BlockedUserView>
}

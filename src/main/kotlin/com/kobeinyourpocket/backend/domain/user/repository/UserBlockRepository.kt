package com.kobeinyourpocket.backend.domain.user.repository

import com.kobeinyourpocket.backend.domain.user.model.User
import com.kobeinyourpocket.backend.domain.user.model.UserBlock

/** [リポジトリ] write port（command）。ブロック中のユーザー一覧（read）は application.user.query.UserBlockQuery。 */
interface UserBlockRepository {
    /**
     * ブロックを保存する。同じ組がすでにあれば何もしない（冪等）。
     *
     * 事前の存在確認だけでは同時送信をすり抜けるため、実装は DB の一意制約違反も
     * 「すでにある」と読み替える。
     */
    fun saveIfAbsent(block: UserBlock)

    /** ブロックを解除する。該当が無くても例外にしない（冪等）。 */
    fun delete(
        blockerId: User.Id,
        blockedId: User.Id,
    )

    /**
     * 指定したユーザーが関わるブロック（した側・された側の両方）を全件削除する。退会処理から呼ぶ。
     * 該当 0 件でも例外にしない。
     *
     * @return 削除した件数
     */
    fun deleteByUserId(userId: User.Id): Int
}

package com.kobeinyourpocket.backend.application.user.query

import java.time.Instant

/** ブロック中のユーザー 1 件分の読みモデル（#215）。解除 UI に表示名・アイコンを出すため。 */
data class BlockedUserView(
    val id: String,
    val name: String,
    val iconUrl: String?,
    val blockedAt: Instant,
)

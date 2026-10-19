package com.kobeinyourpocket.backend.infrastructure.rest.user

import com.kobeinyourpocket.backend.application.user.query.BlockedUserView
import java.time.Instant

/** `GET /api/v1/users/me/blocks` の 1 件（#215）。 */
data class BlockedUserResponse(
    val id: String,
    val name: String,
    val iconUrl: String?,
    val blockedAt: Instant,
) {
    companion object {
        fun from(view: BlockedUserView): BlockedUserResponse =
            BlockedUserResponse(
                id = view.id,
                name = view.name,
                iconUrl = view.iconUrl,
                blockedAt = view.blockedAt,
            )
    }
}

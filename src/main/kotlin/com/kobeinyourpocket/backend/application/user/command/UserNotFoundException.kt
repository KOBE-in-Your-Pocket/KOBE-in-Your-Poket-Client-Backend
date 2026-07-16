package com.kobeinyourpocket.backend.application.user.command

import com.kobeinyourpocket.backend.domain.user.model.User

/** 指定 [User.Id] のユーザーが存在しない場合の例外（REST では 404）。 */
class UserNotFoundException(
    id: User.Id,
) : RuntimeException("User not found: $id")

package com.kobeinyourpocket.backend.domain.user.repository

import com.kobeinyourpocket.backend.domain.user.model.User

/**
 * [リポジトリ] write port（command）。Apple の refresh token の保管（#213）。
 *
 * 退会時に Apple のトークンを失効させるためだけに持つ。1 ユーザー 1 件で、サインインのたびに最新の値へ置き換える。
 */
interface AppleRefreshTokenRepository {
    fun save(
        userId: User.Id,
        refreshToken: String,
    )

    fun findByUserId(userId: User.Id): String?

    fun deleteByUserId(userId: User.Id)
}

package com.kobeinyourpocket.backend.infrastructure.rest.user

import com.kobeinyourpocket.backend.application.user.query.UserListItemView
import com.kobeinyourpocket.backend.application.user.query.UserPageView
import java.time.Instant

/**
 * `GET /api/v1/users` のレスポンス封筒（#151）。
 *
 * `data` + `meta` の形は避難所一覧（[com.kobeinyourpocket.backend.infrastructure.rest.evacuation.ShelterListResponse]）
 * に合わせる。`meta` にページ情報を載せるのは、管理画面のページャが総件数を必要とするため。
 */
data class UserListResponse(
    val data: List<UserListItemResponse>,
    val meta: UserListMetaResponse,
) {
    companion object {
        fun from(view: UserPageView): UserListResponse =
            UserListResponse(
                data = view.users.map(UserListItemResponse::from),
                meta =
                    UserListMetaResponse(
                        page = view.page,
                        size = view.size,
                        totalElements = view.totalElements,
                        totalPages = view.totalPages,
                    ),
            )
    }
}

/**
 * 一覧 1 件分。`PublicUser` 契約 `{ id, name, iconUrl }` に登録日時を足した形。
 *
 * ロールは含めない（[UserListItemView] のコメント参照）。
 */
data class UserListItemResponse(
    val id: String,
    val name: String,
    val iconUrl: String?,
    val createdAt: Instant,
) {
    companion object {
        fun from(view: UserListItemView): UserListItemResponse =
            UserListItemResponse(
                id = view.id,
                name = view.name,
                iconUrl = view.iconUrl,
                createdAt = view.createdAt,
            )
    }
}

data class UserListMetaResponse(
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

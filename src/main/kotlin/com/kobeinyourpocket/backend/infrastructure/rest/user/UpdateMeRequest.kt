package com.kobeinyourpocket.backend.infrastructure.rest.user

import com.kobeinyourpocket.backend.application.user.command.UpdateOwnProfileService.IconUpdate
import com.kobeinyourpocket.backend.domain.user.model.User
import jakarta.validation.constraints.Size

/**
 * 本人のプロフィール更新リクエスト（#179）。
 *
 * 部分更新（PATCH）。送られたフィールドだけを変更する。
 *
 * [iconUrl] は「未設定に戻す」専用で、URL の指定は受け付けない（#184）。差し替えは
 * `POST /api/v1/users/me/icon` に画像を送る経路だけにしてある。URL を受けると、
 * 他の利用者に表示される画像として任意の外部 URL を保存できてしまうため。
 *
 * | iconUrl | 意味 |
 * | --- | --- |
 * | キー無し / null | アイコンを変更しない |
 * | `""`（空文字） | アイコンを未設定に戻す |
 * | それ以外 | 400（差し替えは `POST /api/v1/users/me/icon`） |
 *
 * 表示名は空にできない（[User] の不変条件）ため、変更しないときはキーごと省略する。
 */
data class UpdateMeRequest(
    @field:Size(min = 1, max = User.MAX_NAME_LENGTH)
    val name: String? = null,
    /**
     * 空文字のみ受け付ける（アイコンを未設定に戻す）。
     *
     * `@Size(max = 0)` は「長さ 0 以下」＝空文字だけを通す。null は検証対象外なので
     * 「キー無し・null = 変更しない」はそのまま成立する。
     */
    @field:Size(
        max = 0,
        message = "iconUrl は空文字のみ指定できます（画像の差し替えは POST /api/v1/users/me/icon）",
    )
    val iconUrl: String? = null,
) {
    /** [iconUrl] をユースケースの指示へ変換する。URL 指定は検証で弾かれるためここには来ない。 */
    fun toIconUpdate(): IconUpdate =
        when (iconUrl) {
            null -> IconUpdate.Unchanged
            else -> IconUpdate.Clear
        }
}

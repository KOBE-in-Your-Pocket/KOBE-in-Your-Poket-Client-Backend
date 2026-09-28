package com.kobeinyourpocket.backend.infrastructure.rest.user

import com.kobeinyourpocket.backend.application.user.command.UpdateOwnProfileService.IconUpdate
import com.kobeinyourpocket.backend.domain.user.model.User
import jakarta.validation.constraints.Size

/**
 * 本人のプロフィール更新リクエスト（#179）。
 *
 * 部分更新（PATCH）。送られたフィールドだけを変更する。
 *
 * [iconUrl] は 3 状態を区別する必要があるため、空文字を「未設定に戻す」に割り当てている。
 * JSON の「キー無し」と「null」を区別する仕組み（JsonNullable 等）を入れずに済ませるための取り決め。
 *
 * | iconUrl | 意味 |
 * | --- | --- |
 * | キー無し / null | アイコンを変更しない |
 * | `""`（空文字） | アイコンを未設定に戻す |
 * | URL 文字列 | その URL に差し替える |
 *
 * 表示名は空にできない（[User] の不変条件）ため、変更しないときはキーごと省略する。
 */
data class UpdateMeRequest(
    @field:Size(min = 1, max = User.MAX_NAME_LENGTH)
    val name: String? = null,
    val iconUrl: String? = null,
) {
    /** [iconUrl] の 3 状態をユースケースの指示へ変換する。 */
    fun toIconUpdate(): IconUpdate =
        when {
            iconUrl == null -> IconUpdate.Unchanged
            iconUrl.isBlank() -> IconUpdate.Clear
            else -> IconUpdate.Set(iconUrl)
        }
}

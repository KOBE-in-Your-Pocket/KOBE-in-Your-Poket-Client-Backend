package com.kobeinyourpocket.backend.application.user.command

import com.kobeinyourpocket.backend.domain.user.model.User
import com.kobeinyourpocket.backend.domain.user.repository.UserRepository
import com.kobeinyourpocket.backend.domain.user.vo.UserIcon
import org.springframework.stereotype.Service

/**
 * 本人によるプロフィール（表示名・アイコン）更新ユースケース（#179）。
 *
 * 更新対象は呼び出し側が JWT の subject から解決した [User.Id] で、ここでは本人判定をしない。
 * 他人の id を指定する経路（パスパラメータ等）を作らないことで成立させている。
 *
 * アイコンは「変更しない」と「未設定に戻す」を区別する必要があるため、[IconUpdate] で表す。
 * 表示名は空にできない（[User] の不変条件）ため、省略＝変更しないとして扱う。
 */
@Service
class UpdateOwnProfileService(
    private val userRepository: UserRepository,
) {
    /** アイコンの更新指示。 */
    sealed interface IconUpdate {
        /** 変更しない（リクエストにアイコンの指定が無い）。 */
        data object Unchanged : IconUpdate

        /** 未設定に戻す（既定アイコン表示に戻す）。 */
        data object Clear : IconUpdate

        /** 指定 URL に差し替える。 */
        data class Set(
            val url: String,
        ) : IconUpdate
    }

    /**
     * @param name 新しい表示名。null なら変更しない
     * @param icon アイコンの更新指示
     * @throws UserNotFoundException プロフィール行が無い場合
     * @throws IllegalArgumentException 表示名が空・長すぎる場合（[User] の不変条件）
     */
    fun execute(
        userId: User.Id,
        name: String? = null,
        icon: IconUpdate = IconUpdate.Unchanged,
    ): User {
        val current = userRepository.findById(userId) ?: throw UserNotFoundException(userId)

        val updated =
            current.updateProfile(
                name = name ?: current.name,
                icon =
                    when (icon) {
                        is IconUpdate.Unchanged -> current.icon
                        is IconUpdate.Clear -> null
                        is IconUpdate.Set -> UserIcon.of(icon.url)
                    },
            )

        return userRepository.save(updated)
    }
}

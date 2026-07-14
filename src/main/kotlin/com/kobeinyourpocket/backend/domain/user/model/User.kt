package com.kobeinyourpocket.backend.domain.user.model

import com.kobeinyourpocket.backend.domain.user.vo.PublicUser
import com.kobeinyourpocket.backend.domain.user.vo.UserIcon
import java.time.Instant
import java.util.UUID

/**
 * [集約ルート] アプリ側ユーザープロフィール。
 *
 * id は Supabase Auth の user id（JWT `sub`）と同一。
 * パスワード等のクレデンシャルは持たない（Supabase Auth が管理）。
 * ロールは [com.kobeinyourpocket.backend.domain.user.vo.Role] として JWT から解決し、
 * 本集約・users テーブルには含めない。
 */
data class User(
    val id: Id,
    val name: String,
    val icon: UserIcon? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    /**
     * ユーザーの識別子。
     *
     * エンティティの同一性そのものであり、エンティティを構成するため本ファイルに同居させる。
     * Supabase Auth の user id（JWT `sub`）と同一の UUID。[Companion.of] が復元入口。
     */
    @JvmInline
    value class Id private constructor(
        val value: UUID,
    ) {
        override fun toString(): String = value.toString()

        companion object {
            fun of(value: UUID): Id = Id(value)

            /**
             * UUID 文字列から復元する。不正な形式は [IllegalArgumentException] をスローする。
             */
            fun of(value: String): Id =
                try {
                    Id(UUID.fromString(value))
                } catch (e: IllegalArgumentException) {
                    throw IllegalArgumentException("Invalid User.Id format: $value", e)
                }
        }
    }

    init {
        require(name.isNotBlank()) { "user name must not be blank" }
        require(name.length <= MAX_NAME_LENGTH) {
            "user name must be at most $MAX_NAME_LENGTH characters, got ${name.length}"
        }
    }

    /** Client 契約の [PublicUser] へ射影する（wire 形の iconUrl は [UserIcon.url]）。 */
    fun toPublicUser(): PublicUser = PublicUser(name = name, iconUrl = icon?.url)

    /**
     * 表示名・アイコンを更新する。id / createdAt は維持する。
     *
     * [updatedAt] は DI や固定値での上書きが必要なテスト以外では省略してよい。
     */
    fun updateProfile(
        name: String,
        icon: UserIcon? = this.icon,
        updatedAt: Instant = Instant.now(),
    ): User =
        copy(
            name = name,
            icon = icon,
            updatedAt = updatedAt,
        )

    companion object {
        const val MAX_NAME_LENGTH = PublicUser.MAX_NAME_LENGTH

        /**
         * サインアップ直後など、Auth ユーザー作成と同期してプロフィール行を作る。
         *
         * [id] は Supabase Auth が発行した UUID。[createdAt]/[updatedAt] は
         * テスト以外では省略してよい。
         */
        fun create(
            id: Id,
            name: String,
            icon: UserIcon? = null,
            createdAt: Instant = Instant.now(),
            updatedAt: Instant = createdAt,
        ): User =
            User(
                id = id,
                name = name,
                icon = icon,
                createdAt = createdAt,
                updatedAt = updatedAt,
            )
    }
}

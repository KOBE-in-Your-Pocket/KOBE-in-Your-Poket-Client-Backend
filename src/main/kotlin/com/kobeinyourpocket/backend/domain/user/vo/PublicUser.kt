package com.kobeinyourpocket.backend.domain.user.vo

/**
 * [値オブジェクト] Client 向け公開ユーザー射影。
 *
 * Client 契約の `PublicUser { name, iconUrl }` に対応する。
 * クレデンシャルやロールは含まない。
 */
data class PublicUser(
    val name: String,
    val iconUrl: String? = null,
) {
    init {
        require(name.isNotBlank()) { "public user name must not be blank" }
        require(name.length <= MAX_NAME_LENGTH) {
            "public user name must be at most $MAX_NAME_LENGTH characters, got ${name.length}"
        }
    }

    companion object {
        const val MAX_NAME_LENGTH = 100
    }
}

package com.kobeinyourpocket.backend.infrastructure.security

import com.kobeinyourpocket.backend.domain.user.vo.Role
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.request.RequestPostProcessor

/**
 * MockMvc リクエストに指定ロールの JWT 認証を付与する（#90）。
 *
 * 統合テストで書き込み系エンドポイントを叩くときに使う。
 * 実トークンの署名検証を通したテストは [WriteAuthorizationTest] が担う。
 *
 * [subject] を渡すと JWT の `sub` を固定できる。レビュー本人操作（#86）のように
 * `sub` が誰かで結果が変わるエンドポイントで使う。
 *
 * 既定値を明示しているのは、spring-security-test の既定 `sub` が `"user"` という
 * UUID でない文字列で、Supabase の user id（UUID）を期待する側が 400 になるため。
 */
fun withRole(
    role: Role,
    subject: String = DEFAULT_SUBJECT,
): RequestPostProcessor =
    jwt()
        .authorities(SimpleGrantedAuthority(role.authority))
        .jwt { builder -> builder.subject(subject) }

/** [withRole] の既定 `sub`。Supabase の user id と同じ UUID 形式であればよい。 */
const val DEFAULT_SUBJECT: String = "00000000-0000-0000-0000-0000000000ff"

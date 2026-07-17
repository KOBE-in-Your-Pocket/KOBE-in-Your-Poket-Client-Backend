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
 */
fun withRole(role: Role): RequestPostProcessor = jwt().authorities(SimpleGrantedAuthority(role.authority))

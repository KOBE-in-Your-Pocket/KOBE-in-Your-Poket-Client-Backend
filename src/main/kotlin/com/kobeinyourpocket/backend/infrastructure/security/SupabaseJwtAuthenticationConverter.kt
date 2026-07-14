package com.kobeinyourpocket.backend.infrastructure.security

import com.kobeinyourpocket.backend.domain.user.vo.Role
import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter

/**
 * Supabase JWT → Spring Authentication。
 *
 * ロールの正は `app_metadata.role`（[Role.claimValue]）。未設定・未対応は一般（[Role.GENERAL]）。
 * Spring の `hasRole("OPERATOR")` は `ROLE_OPERATOR` を参照する。
 */
class SupabaseJwtAuthenticationConverter : Converter<Jwt, AbstractAuthenticationToken> {
    private val delegate =
        JwtAuthenticationConverter().apply {
            setJwtGrantedAuthoritiesConverter { jwt -> authoritiesFrom(jwt) }
        }

    override fun convert(source: Jwt): AbstractAuthenticationToken = checkNotNull(delegate.convert(source))

    companion object {
        const val APP_METADATA_CLAIM = "app_metadata"
        const val ROLE_CLAIM_KEY = "role"

        fun authoritiesFrom(jwt: Jwt): Collection<GrantedAuthority> {
            val role =
                jwt
                    .getClaimAsMap(APP_METADATA_CLAIM)
                    ?.get(ROLE_CLAIM_KEY)
                    ?.toString()
                    ?.let(Role::of)
                    ?: Role.GENERAL
            return listOf(SimpleGrantedAuthority(role.authority))
        }
    }
}

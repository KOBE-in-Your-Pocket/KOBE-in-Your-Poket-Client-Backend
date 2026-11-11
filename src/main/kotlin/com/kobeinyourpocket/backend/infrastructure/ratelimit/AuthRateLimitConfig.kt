package com.kobeinyourpocket.backend.infrastructure.ratelimit

import com.kobeinyourpocket.backend.application.user.auth.LoginAttemptLimiter
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import tools.jackson.databind.ObjectMapper

/** ログイン・サインアップのレート制限の組み立て（#244）。 */
@Configuration
class AuthRateLimitConfig(
    private val properties: AuthRateLimitProperties,
) {
    @Bean
    fun loginAttemptLimiter(): LoginAttemptLimiter =
        InMemoryLoginAttemptLimiter(
            FixedWindowRateLimiter(
                maxHits = properties.loginFailure.maxAttempts,
                window = properties.loginFailure.window,
                maxKeys = properties.maxTrackedKeys,
            ),
        )

    /**
     * Spring Security より前で弾く（対象パスは認証不要の入り口なので順序の影響は無い）。
     * `@Component` にしないのは、`@WebMvcTest` が Filter Bean を拾って他テストの文脈を壊さないため。
     */
    @Bean
    fun authIpRateLimitFilter(objectMapper: ObjectMapper): FilterRegistrationBean<AuthIpRateLimitFilter> {
        val filter =
            AuthIpRateLimitFilter(
                limiter =
                    FixedWindowRateLimiter(
                        maxHits = properties.ip.maxAttempts,
                        window = properties.ip.window,
                        maxKeys = properties.maxTrackedKeys,
                    ),
                objectMapper = objectMapper,
            )
        return FilterRegistrationBean(filter).apply {
            addUrlPatterns(*AuthIpRateLimitFilter.TARGET_PATHS.toTypedArray())
            order = Ordered.HIGHEST_PRECEDENCE + 10
        }
    }
}

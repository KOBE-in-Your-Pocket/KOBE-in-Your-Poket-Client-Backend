package com.kobeinyourpocket.backend.infrastructure.ratelimit

import com.kobeinyourpocket.backend.application.tourism.command.SpotViewDeduplicator
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import tools.jackson.databind.ObjectMapper

/** 閲覧を数える API の重複まとめとレート制限の組み立て（#260）。 */
@Configuration
class SpotViewRateLimitConfig(
    private val properties: SpotViewRateLimitProperties,
) {
    @Bean
    fun spotViewDeduplicator(): SpotViewDeduplicator =
        InMemorySpotViewDeduplicator(
            FixedWindowRateLimiter(
                maxHits = 1,
                window = properties.dedupeWindow,
                maxKeys = properties.maxTrackedKeys,
            ),
        )

    /** [AuthRateLimitConfig.authIpRateLimitFilter] と同じく、`@Component` にせず Spring Security より前に置く。 */
    @Bean
    fun spotViewIpRateLimitFilter(objectMapper: ObjectMapper): FilterRegistrationBean<SpotViewIpRateLimitFilter> {
        val filter =
            SpotViewIpRateLimitFilter(
                limiter =
                    FixedWindowRateLimiter(
                        maxHits = properties.ip.maxRequests,
                        window = properties.ip.window,
                        maxKeys = properties.maxTrackedKeys,
                    ),
                objectMapper = objectMapper,
            )
        return FilterRegistrationBean(filter).apply {
            addUrlPatterns(SpotViewIpRateLimitFilter.URL_PATTERN)
            order = Ordered.HIGHEST_PRECEDENCE + 10
        }
    }
}

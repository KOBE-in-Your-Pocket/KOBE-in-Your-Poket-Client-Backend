package com.kobeinyourpocket.backend

import com.kobeinyourpocket.backend.infrastructure.apple.AppleSignInProperties
import com.kobeinyourpocket.backend.infrastructure.ratelimit.AuthRateLimitProperties
import com.kobeinyourpocket.backend.infrastructure.security.SupabaseJwtProperties
import com.kobeinyourpocket.backend.infrastructure.storage.MediaStorageProperties
import com.kobeinyourpocket.backend.infrastructure.supabase.SupabaseAuthProperties
import com.kobeinyourpocket.backend.infrastructure.weather.YahooWeatherProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(
    SupabaseAuthProperties::class,
    SupabaseJwtProperties::class,
    MediaStorageProperties::class,
    AppleSignInProperties::class,
    YahooWeatherProperties::class,
    AuthRateLimitProperties::class,
)
class KobeBackendApplication

fun main(args: Array<String>) {
    runApplication<KobeBackendApplication>(*args)
}

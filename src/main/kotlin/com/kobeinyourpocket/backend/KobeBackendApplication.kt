package com.kobeinyourpocket.backend

import com.kobeinyourpocket.backend.infrastructure.security.SupabaseJwtProperties
import com.kobeinyourpocket.backend.infrastructure.storage.MediaStorageProperties
import com.kobeinyourpocket.backend.infrastructure.supabase.SupabaseAuthProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(
    SupabaseAuthProperties::class,
    SupabaseJwtProperties::class,
    MediaStorageProperties::class,
)
class KobeBackendApplication

fun main(args: Array<String>) {
    runApplication<KobeBackendApplication>(*args)
}

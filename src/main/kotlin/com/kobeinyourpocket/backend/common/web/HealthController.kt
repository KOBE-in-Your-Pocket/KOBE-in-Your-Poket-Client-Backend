package com.kobeinyourpocket.backend.common.web

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime

@RestController
@RequestMapping("/api")
class HealthController {
    @GetMapping("/ping")
    fun ping(): Map<String, Any> =
        mapOf(
            "status" to "ok",
            "service" to "kobe-backend",
            "timestamp" to OffsetDateTime.now().toString(),
        )
}

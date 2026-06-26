package com.kobeinyourpocket.backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class KobeBackendApplication

fun main(args: Array<String>) {
    runApplication<KobeBackendApplication>(*args)
}

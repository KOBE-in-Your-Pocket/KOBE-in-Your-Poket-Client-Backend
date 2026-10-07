package com.kobeinyourpocket.backend.infrastructure.rest.common

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * API 契約（Swagger UI / OpenAPI JSON）のメタ情報（#82）。
 *
 * - Swagger UI の Authorize に Supabase の access_token を入れると、
 *   書き込み系（[com.kobeinyourpocket.backend.infrastructure.security.SecurityConfig]）も試せる
 * - 全操作に Bearer 要件を付けるが、GET 系は未認証でも呼べる（トークンは任意）
 * - servers はリクエストから決まる（Caddy 経由なら https のホスト名。application.yml の
 *   forward-headers-strategy を参照）
 */
@Configuration
class OpenApiConfig {
    @Bean
    fun openApi(): OpenAPI =
        OpenAPI()
            .info(
                Info()
                    .title("KOBE in Your Pocket API")
                    .version("v1")
                    .description(
                        "神戸の観光・マナー・防災情報を多言語で提供する API。" +
                            "GET 系は公開。書き込み系は Supabase の access_token（Bearer）が必要。",
                    ),
            ).components(
                Components().addSecuritySchemes(
                    BEARER_SCHEME,
                    SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("POST /api/v1/auth/login 等で得た Supabase の access_token"),
                ),
            ).addSecurityItem(SecurityRequirement().addList(BEARER_SCHEME))

    companion object {
        private const val BEARER_SCHEME = "bearerAuth"
    }
}

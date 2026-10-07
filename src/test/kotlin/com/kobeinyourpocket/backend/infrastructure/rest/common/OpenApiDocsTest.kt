package com.kobeinyourpocket.backend.infrastructure.rest.common

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.Test

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiDocsTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `OpenAPI JSON は未認証で取得でき 既存エンドポイントと Bearer 方式を含む`() {
        mockMvc
            .perform(get("/v3/api-docs"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.info.title").value("KOBE in Your Pocket API"))
            .andExpect(jsonPath("$.paths['/api/v1/tourism/spots']").exists())
            .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
    }

    @Test
    fun `プロキシ経由の https リクエストなら servers は公開側の URL になる`() {
        mockMvc
            .perform(
                get("/v3/api-docs")
                    .with { request ->
                        // MockMvc は RemoteIpValve を通らないため、プロキシ解釈後の状態を直接作る
                        request.scheme = "https"
                        request.serverName = "18-181-34-28.sslip.io"
                        request.serverPort = 443
                        request
                    },
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.servers[0].url").value("https://18-181-34-28.sslip.io"))
    }

    @Test
    fun `Swagger UI は未認証で開ける`() {
        mockMvc
            .perform(get("/swagger-ui/index.html"))
            .andExpect(status().isOk)
    }
}

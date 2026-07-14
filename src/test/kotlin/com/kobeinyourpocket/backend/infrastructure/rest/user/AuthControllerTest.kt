package com.kobeinyourpocket.backend.infrastructure.rest.user

import com.kobeinyourpocket.backend.application.user.auth.AuthGatewayException
import com.kobeinyourpocket.backend.application.user.auth.AuthSession
import com.kobeinyourpocket.backend.application.user.command.AuthCommandResult
import com.kobeinyourpocket.backend.application.user.command.RefreshSessionService
import com.kobeinyourpocket.backend.application.user.command.SignInService
import com.kobeinyourpocket.backend.application.user.command.SignOutService
import com.kobeinyourpocket.backend.application.user.command.SignUpService
import com.kobeinyourpocket.backend.domain.user.model.PublicUser
import com.kobeinyourpocket.backend.domain.user.model.User
import com.kobeinyourpocket.backend.infrastructure.rest.common.GlobalExceptionHandler
import org.mockito.BDDMockito.given
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID
import kotlin.test.Test

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(AuthController::class)
@Import(GlobalExceptionHandler::class)
class AuthControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var signUpService: SignUpService

    @MockitoBean
    private lateinit var signInService: SignInService

    @MockitoBean
    private lateinit var refreshSessionService: RefreshSessionService

    @MockitoBean
    private lateinit var signOutService: SignOutService

    private val userId = User.Id.of(UUID.fromString("11111111-1111-1111-1111-111111111111"))

    @Test
    fun `POST signup は 201 とセッション JSON を返す`() {
        given(signUpService.execute("a@example.com", "password1", "Alice")).willReturn(
            AuthCommandResult(
                session =
                    AuthSession(
                        userId = userId,
                        accessToken = "access",
                        refreshToken = "refresh",
                        expiresIn = 3600,
                        tokenType = "bearer",
                    ),
                user = PublicUser(id = userId, name = "Alice"),
            ),
        )

        mockMvc
            .perform(
                post("/api/v1/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"email":"a@example.com","password":"password1","name":"Alice"}
                        """.trimIndent(),
                    ),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.accessToken").value("access"))
            .andExpect(jsonPath("$.user.id").value(userId.toString()))
            .andExpect(jsonPath("$.user.name").value("Alice"))
    }

    @Test
    fun `POST login で AuthGateway の 400 は統一エラーになる`() {
        given(signInService.execute("a@example.com", "bad")).willThrow(
            AuthGatewayException(status = 400, message = "Invalid login credentials"),
        )

        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"email":"a@example.com","password":"bad"}"""),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Invalid login credentials"))
    }

    @Test
    fun `POST logout は Bearer を渡して 204`() {
        mockMvc
            .perform(
                post("/api/v1/auth/logout")
                    .header("Authorization", "Bearer access-token"),
            ).andExpect(status().isNoContent)

        verify(signOutService).execute("access-token")
    }
}

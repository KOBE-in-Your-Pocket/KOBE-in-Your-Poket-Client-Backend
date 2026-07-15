package com.kobeinyourpocket.backend.infrastructure.rest.user

import com.kobeinyourpocket.backend.application.user.auth.AuthGatewayException
import com.kobeinyourpocket.backend.application.user.auth.AuthSession
import com.kobeinyourpocket.backend.application.user.command.AuthCommandResult
import com.kobeinyourpocket.backend.application.user.command.DeleteUserService
import com.kobeinyourpocket.backend.application.user.command.RefreshSessionService
import com.kobeinyourpocket.backend.application.user.command.SignInService
import com.kobeinyourpocket.backend.application.user.command.SignOutService
import com.kobeinyourpocket.backend.application.user.command.SignUpService
import com.kobeinyourpocket.backend.application.user.command.UserNotFoundException
import com.kobeinyourpocket.backend.domain.user.model.PublicUser
import com.kobeinyourpocket.backend.domain.user.model.User
import com.kobeinyourpocket.backend.infrastructure.rest.common.GlobalExceptionHandler
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.willDoNothing
import org.mockito.BDDMockito.willThrow
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
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

    @MockitoBean
    private lateinit var deleteUserService: DeleteUserService

    private val userId = User.Id.of(UUID.fromString("11111111-1111-1111-1111-111111111111"))

    private fun session(access: String = "access") =
        AuthSession(
            userId = userId,
            accessToken = access,
            refreshToken = "refresh",
            expiresIn = 3600,
            tokenType = "bearer",
        )

    // --- signup ---

    @Test
    fun `POST signup は 201 とセッション JSON を返す`() {
        given(signUpService.execute("a@example.com", "password1", "Alice")).willReturn(
            AuthCommandResult(
                session = session(),
                user = PublicUser(id = userId, name = "Alice"),
            ),
        )

        mockMvc
            .perform(
                post("/api/v1/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"email":"a@example.com","password":"password1","name":"Alice"}"""),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.accessToken").value("access"))
            .andExpect(jsonPath("$.user.id").value(userId.toString()))
            .andExpect(jsonPath("$.user.name").value("Alice"))
    }

    @Test
    fun `POST signup でメールが空なら 400`() {
        mockMvc
            .perform(
                post("/api/v1/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"email":"","password":"password1","name":"Alice"}"""),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `POST signup でパスワードが短すぎたら 400`() {
        mockMvc
            .perform(
                post("/api/v1/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"email":"a@example.com","password":"abc","name":"Alice"}"""),
            ).andExpect(status().isBadRequest)
    }

    // --- login ---

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

    // --- refresh ---

    @Test
    fun `POST refresh 成功は 200 とセッション JSON を返す`() {
        given(refreshSessionService.execute("rt")).willReturn(
            AuthCommandResult(session = session("new-token"), user = null),
        )

        mockMvc
            .perform(
                post("/api/v1/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"refreshToken":"rt"}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").value("new-token"))
    }

    @Test
    fun `POST refresh で AuthGateway の 401 は統一エラーになる`() {
        given(refreshSessionService.execute("expired")).willThrow(
            AuthGatewayException(status = 401, message = "Invalid refresh token"),
        )

        mockMvc
            .perform(
                post("/api/v1/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"refreshToken":"expired"}"""),
            ).andExpect(status().isUnauthorized)
    }

    // --- logout ---

    @Test
    fun `POST logout は Bearer を渡して 204`() {
        mockMvc
            .perform(
                post("/api/v1/auth/logout")
                    .header("Authorization", "Bearer access-token"),
            ).andExpect(status().isNoContent)

        verify(signOutService).execute("access-token")
    }

    @Test
    fun `POST logout で Authorization ヘッダが無ければ 401`() {
        mockMvc
            .perform(post("/api/v1/auth/logout"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `POST logout で Bearer でない Authorization は 401`() {
        mockMvc
            .perform(
                post("/api/v1/auth/logout")
                    .header("Authorization", "Basic dXNlcjpwYXNz"),
            ).andExpect(status().isUnauthorized)
    }

    // --- deleteUser ---

    @Test
    fun `DELETE users-userId は 204 を返す`() {
        willDoNothing().given(deleteUserService).execute(userId)

        mockMvc
            .perform(delete("/api/v1/auth/users/${userId.value}"))
            .andExpect(status().isNoContent)

        verify(deleteUserService).execute(userId)
    }

    @Test
    fun `DELETE users-userId でユーザーが存在しなければ 404`() {
        willThrow(UserNotFoundException(userId)).given(deleteUserService).execute(userId)

        mockMvc
            .perform(delete("/api/v1/auth/users/${userId.value}"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.status").value(404))
    }

    @Test
    fun `DELETE users-userId で無効な UUID なら 400`() {
        mockMvc
            .perform(delete("/api/v1/auth/users/not-a-uuid"))
            .andExpect(status().isBadRequest)
    }
}

package com.kobeinyourpocket.backend.infrastructure.security

import com.kobeinyourpocket.backend.application.tourism.command.PostReviewService
import com.kobeinyourpocket.backend.application.tourism.command.RegisterSpotService
import com.kobeinyourpocket.backend.application.tourism.command.UpdateReviewService
import com.kobeinyourpocket.backend.application.tourism.command.UpdateSpotService
import com.kobeinyourpocket.backend.application.user.command.DeleteUserService
import com.kobeinyourpocket.backend.application.user.command.SignOutService
import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.domain.tourism.review.model.Review
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewAuthor
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewId
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewRating
import com.kobeinyourpocket.backend.domain.tourism.spot.model.Spot
import com.kobeinyourpocket.backend.domain.tourism.spot.model.SpotWithLocalizations
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.Coordinates
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.Genre
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotLocalization
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotLocalizations
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotMedia
import com.kobeinyourpocket.backend.domain.user.model.User
import com.kobeinyourpocket.backend.domain.user.vo.Role
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import org.mockito.BDDMockito.given
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
import java.util.Date
import java.util.UUID
import kotlin.test.Test

/**
 * 書き込み系エンドポイントの認可（#90）を 未認証 / 一般 / 運営 / 管理 で検証する。
 *
 * application サービスはモックし、Security フィルタ層の判定
 * （401 / 403 / 通過）と統一エラー形式の応答だけを対象にする。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WriteAuthorizationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Value("\${supabase.jwt.secret}")
    private lateinit var jwtSecret: String

    @MockitoBean
    private lateinit var postReviewService: PostReviewService

    @MockitoBean
    private lateinit var updateReviewService: UpdateReviewService

    @MockitoBean
    private lateinit var registerSpotService: RegisterSpotService

    @MockitoBean
    private lateinit var updateSpotService: UpdateSpotService

    @MockitoBean
    private lateinit var signOutService: SignOutService

    @MockitoBean
    private lateinit var deleteUserService: DeleteUserService

    // ---- GET 系は公開のまま ----

    @Test
    fun `GET スポット一覧は未認証でも 200`() {
        mockMvc
            .perform(get("/api/v1/tourism/spots"))
            .andExpect(status().isOk)
    }

    // ---- 認証の入り口（signup, login, google, refresh）は公開 ----

    @Test
    fun `auth の入り口 POST は未認証でも Security を通過する`() {
        // 空ボディで 400（バリデーション）まで到達する = 401 で拒否されていない
        listOf("signup", "login", "google", "refresh").forEach { path ->
            mockMvc
                .perform(
                    post("/api/v1/auth/$path")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"),
                ).andExpect(status().isBadRequest)
        }
    }

    // ---- POST /api/v1/auth/logout: 認証必須 ----

    @Test
    fun `logout は未認証だと 401 を統一エラー形式で返す`() {
        mockMvc
            .perform(post("/api/v1/auth/logout"))
            .andExpectUnauthorizedApiError()
    }

    @Test
    fun `logout は一般ユーザーで 204`() {
        val token = jwt(Role.GENERAL)
        mockMvc
            .perform(
                post("/api/v1/auth/logout")
                    .header("Authorization", "Bearer $token"),
            ).andExpect(status().isNoContent)

        verify(signOutService).execute(token)
    }

    // ---- レビュー投稿・更新: 認証済みユーザー（一般可） ----

    @Test
    fun `レビュー投稿は未認証だと 401`() {
        mockMvc
            .perform(
                post("/api/v1/tourism/spots/kobe-port-tower/reviews")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(reviewBody),
            ).andExpectUnauthorizedApiError()
    }

    @Test
    fun `レビュー投稿は一般ユーザーで 201`() {
        stubPostReview()
        mockMvc
            .perform(
                post("/api/v1/tourism/spots/kobe-port-tower/reviews")
                    .header("Authorization", "Bearer ${jwt(Role.GENERAL)}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(reviewBody),
            ).andExpect(status().isCreated)
    }

    @Test
    fun `レビュー投稿は運営ロールでも 201（階層で一般操作を包含）`() {
        stubPostReview()
        mockMvc
            .perform(
                post("/api/v1/tourism/spots/kobe-port-tower/reviews")
                    .header("Authorization", "Bearer ${jwt(Role.OPERATOR)}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(reviewBody),
            ).andExpect(status().isCreated)
    }

    @Test
    fun `レビュー更新は未認証だと 401`() {
        mockMvc
            .perform(
                put("/api/v1/tourism/spots/kobe-port-tower/reviews/$REVIEW_ID")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(reviewUpdateBody),
            ).andExpectUnauthorizedApiError()
    }

    @Test
    fun `レビュー更新は一般ユーザーで 200`() {
        given(
            updateReviewService.updateReview(
                reviewId = ReviewId.of(REVIEW_ID),
                rating = ReviewRating.of(5),
                comment = "最高でした",
            ),
        ).willReturn(savedReview)

        mockMvc
            .perform(
                put("/api/v1/tourism/spots/kobe-port-tower/reviews/$REVIEW_ID")
                    .header("Authorization", "Bearer ${jwt(Role.GENERAL)}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(reviewUpdateBody),
            ).andExpect(status().isOk)
    }

    // ---- POST /api/v1/tourism/spots: 運営ロール必須 ----

    @Test
    fun `スポット登録は未認証だと 401 を統一エラー形式で返す`() {
        mockMvc
            .perform(
                post("/api/v1/tourism/spots")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(registerSpotBody),
            ).andExpectUnauthorizedApiError()
    }

    @Test
    fun `スポット登録は一般ユーザーだと 403 を統一エラー形式で返す`() {
        mockMvc
            .perform(
                post("/api/v1/tourism/spots")
                    .header("Authorization", "Bearer ${jwt(Role.GENERAL)}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(registerSpotBody),
            ).andExpectForbiddenApiError()
    }

    @Test
    fun `スポット登録は運営ロールで 201`() {
        stubRegisterSpot()
        mockMvc
            .perform(
                post("/api/v1/tourism/spots")
                    .header("Authorization", "Bearer ${jwt(Role.OPERATOR)}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(registerSpotBody),
            ).andExpect(status().isCreated)
    }

    @Test
    fun `スポット登録は管理者ロールでも 201（階層 ADMIN 大なり OPERATOR）`() {
        stubRegisterSpot()
        mockMvc
            .perform(
                post("/api/v1/tourism/spots")
                    .header("Authorization", "Bearer ${jwt(Role.ADMIN)}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(registerSpotBody),
            ).andExpect(status().isCreated)
    }

    // ---- PUT /api/v1/tourism/spots/{id}: 運営ロール必須 ----

    @Test
    fun `スポット編集は未認証だと 401 を統一エラー形式で返す`() {
        mockMvc
            .perform(
                put("/api/v1/tourism/spots/kobe-port-tower")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(registerSpotBody),
            ).andExpectUnauthorizedApiError()
    }

    @Test
    fun `スポット編集は一般ユーザーだと 403 を統一エラー形式で返す`() {
        mockMvc
            .perform(
                put("/api/v1/tourism/spots/kobe-port-tower")
                    .header("Authorization", "Bearer ${jwt(Role.GENERAL)}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(registerSpotBody),
            ).andExpectForbiddenApiError()
    }

    @Test
    fun `スポット編集は運営ロールで 200`() {
        stubUpdateSpot()
        mockMvc
            .perform(
                put("/api/v1/tourism/spots/kobe-port-tower")
                    .header("Authorization", "Bearer ${jwt(Role.OPERATOR)}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(registerSpotBody),
            ).andExpect(status().isOk)
    }

    @Test
    fun `スポット編集は管理者ロールでも 200（階層 ADMIN 大なり OPERATOR）`() {
        stubUpdateSpot()
        mockMvc
            .perform(
                put("/api/v1/tourism/spots/kobe-port-tower")
                    .header("Authorization", "Bearer ${jwt(Role.ADMIN)}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(registerSpotBody),
            ).andExpect(status().isOk)
    }

    // ---- DELETE /api/v1/auth/users/{id}: ADMIN 専用（@PreAuthorize） ----

    @Test
    fun `ユーザー削除は一般ユーザーだと 403`() {
        mockMvc
            .perform(
                delete("/api/v1/auth/users/$TARGET_USER_ID")
                    .header("Authorization", "Bearer ${jwt(Role.GENERAL)}"),
            ).andExpectForbiddenApiError()
    }

    @Test
    fun `ユーザー削除は運営ロールでも 403（ADMIN 専用）`() {
        mockMvc
            .perform(
                delete("/api/v1/auth/users/$TARGET_USER_ID")
                    .header("Authorization", "Bearer ${jwt(Role.OPERATOR)}"),
            ).andExpectForbiddenApiError()
    }

    @Test
    fun `ユーザー削除は管理者ロールで 204`() {
        mockMvc
            .perform(
                delete("/api/v1/auth/users/$TARGET_USER_ID")
                    .header("Authorization", "Bearer ${jwt(Role.ADMIN)}"),
            ).andExpect(status().isNoContent)

        verify(deleteUserService).execute(User.Id.of(TARGET_USER_ID))
    }

    // ---- 未分類の書き込みは deny-by-default ----

    @Test
    fun `未分類の POST は未認証だと 401`() {
        mockMvc
            .perform(post("/api/v1/nonexistent"))
            .andExpectUnauthorizedApiError()
    }

    @Test
    fun `未分類の POST は一般ユーザーだと 403`() {
        mockMvc
            .perform(
                post("/api/v1/nonexistent")
                    .header("Authorization", "Bearer ${jwt(Role.GENERAL)}"),
            ).andExpectForbiddenApiError()
    }

    // ---- fixtures / helpers ----

    /** 401 と統一エラー形式（§3.3 の4フィールド契約）を検証する。 */
    private fun ResultActions.andExpectUnauthorizedApiError(): ResultActions =
        andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.error").value("Unauthorized"))
            .andExpect(jsonPath("$.message").isNotEmpty)
            .andExpect(jsonPath("$.violations").isArray)

    /** 403 と統一エラー形式（§3.3 の4フィールド契約）を検証する。 */
    private fun ResultActions.andExpectForbiddenApiError(): ResultActions =
        andExpect(status().isForbidden)
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.error").value("Forbidden"))
            .andExpect(jsonPath("$.message").isNotEmpty)
            .andExpect(jsonPath("$.violations").isArray)

    private fun jwt(role: Role): String {
        val claims =
            JWTClaimsSet
                .Builder()
                .subject(UUID.randomUUID().toString())
                .issueTime(Date.from(Instant.now()))
                .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
                .claim(
                    SupabaseJwtAuthenticationConverter.APP_METADATA_CLAIM,
                    mapOf(SupabaseJwtAuthenticationConverter.ROLE_CLAIM_KEY to role.claimValue),
                ).build()
        val signed = SignedJWT(JWSHeader(JWSAlgorithm.HS256), claims)
        signed.sign(MACSigner(jwtSecret.toByteArray(Charsets.UTF_8)))
        return signed.serialize()
    }

    private fun stubPostReview() {
        given(
            postReviewService.postReview(
                spotId = SpotId.of("kobe-port-tower"),
                rating = ReviewRating.of(4),
                comment = "素晴らしい",
                authorName = "Alice",
                language = Language.JA,
            ),
        ).willReturn(savedReview)
    }

    private fun stubRegisterSpot() {
        given(
            registerSpotService.registerSpot(
                genre = Genre.of("landmark"),
                coordinates = Coordinates.of(34.6826, 135.1863),
                media = SpotMedia(imageUrl = "https://example.com/kobe-port-tower.webp"),
                localizations = localizations,
            ),
        ).willReturn(createdSpot)
    }

    private fun stubUpdateSpot() {
        given(
            updateSpotService.updateSpot(
                id = SpotId.of("kobe-port-tower"),
                genre = Genre.of("landmark"),
                coordinates = Coordinates.of(34.6826, 135.1863),
                media = SpotMedia(imageUrl = "https://example.com/kobe-port-tower.webp"),
                localizations = localizations,
            ),
        ).willReturn(createdSpot)
    }

    private val savedReview =
        Review(
            id = ReviewId.of(REVIEW_ID),
            spotId = SpotId.of("kobe-port-tower"),
            rating = ReviewRating.of(4),
            comment = "素晴らしい",
            author = ReviewAuthor(name = "Alice", iconUrl = null),
            createdAt = Instant.parse("2025-11-03T10:00:00Z"),
            language = Language.JA,
        )

    private val reviewBody =
        """
        {
          "rating": 4,
          "comment": "素晴らしい",
          "author": { "name": "Alice" },
          "language": "ja"
        }
        """.trimIndent()

    private val reviewUpdateBody =
        """
        {
          "rating": 5,
          "comment": "最高でした"
        }
        """.trimIndent()

    private val localizations =
        SpotLocalizations.of(
            mapOf(
                Language.JA to
                    SpotLocalization(
                        "神戸ポートタワー",
                        "ランドマーク",
                        "神戸のシンボル。",
                        "9:00-23:00",
                        "神戸市中央区波止場町5-5",
                    ),
                Language.EN to
                    SpotLocalization(
                        "Kobe Port Tower",
                        "Landmark",
                        "The symbol of Kobe.",
                        "9:00-23:00",
                        "5-5 Hatobacho, Chuo-ku, Kobe",
                    ),
                Language.ZH to
                    SpotLocalization(
                        "神户港塔",
                        "地标",
                        "神户的象征。",
                        "9:00-23:00",
                        "神户市中央区波止场町5-5",
                    ),
                Language.KO to
                    SpotLocalization(
                        "고베 포트 타워",
                        "랜드마크",
                        "고베의 상징.",
                        "9:00-23:00",
                        "고베시 주오구 하토바초 5-5",
                    ),
            ),
        )

    private val createdSpot =
        SpotWithLocalizations(
            spot =
                Spot(
                    id = SpotId.of("new-spot-id"),
                    genre = Genre.LANDMARK,
                    coordinates = Coordinates.of(34.6826, 135.1863),
                    media = SpotMedia("https://example.com/kobe-port-tower.webp"),
                    rating = null,
                ),
            localizations = localizations,
        )

    private val registerSpotBody =
        """
        {
          "genre": "landmark",
          "coordinates": { "latitude": 34.6826, "longitude": 135.1863 },
          "imageUrl": "https://example.com/kobe-port-tower.webp",
          "localizations": {
            "ja": {
              "name": "神戸ポートタワー",
              "categoryLabel": "ランドマーク",
              "description": "神戸のシンボル。",
              "businessHours": "9:00-23:00",
              "address": "神戸市中央区波止場町5-5"
            },
            "en": {
              "name": "Kobe Port Tower",
              "categoryLabel": "Landmark",
              "description": "The symbol of Kobe.",
              "businessHours": "9:00-23:00",
              "address": "5-5 Hatobacho, Chuo-ku, Kobe"
            },
            "zh": {
              "name": "神户港塔",
              "categoryLabel": "地标",
              "description": "神户的象征。",
              "businessHours": "9:00-23:00",
              "address": "神户市中央区波止场町5-5"
            },
            "ko": {
              "name": "고베 포트 타워",
              "categoryLabel": "랜드마크",
              "description": "고베의 상징.",
              "businessHours": "9:00-23:00",
              "address": "고베시 주오구 하토바초 5-5"
            }
          }
        }
        """.trimIndent()

    companion object {
        private const val REVIEW_ID = "00000000-0000-0000-0000-000000000001"
        private const val TARGET_USER_ID = "00000000-0000-0000-0000-0000000000aa"
    }
}

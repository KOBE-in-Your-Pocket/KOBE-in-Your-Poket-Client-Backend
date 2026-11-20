package com.kobeinyourpocket.backend.infrastructure.rest.tourism

import com.kobeinyourpocket.backend.application.tourism.query.SpotViewCountQuery
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId
import com.kobeinyourpocket.backend.domain.user.vo.Role
import com.kobeinyourpocket.backend.infrastructure.persistence.tourism.SpotEntity
import com.kobeinyourpocket.backend.infrastructure.persistence.tourism.SpotJpaRepository
import com.kobeinyourpocket.backend.infrastructure.security.withRole
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 観光スポットの閲覧を数える API の統合テスト（#240）。
 * controller → application → JPA → DB を実 Bean で通す。
 *
 * テスト環境は H2（application-test.yml、Hibernate create-drop / Flyway 無効）。
 * H2 には spot への外部キーが無いため、存在確認のあとにスポットが削除された場合の 404 は
 * [com.kobeinyourpocket.backend.application.tourism.command.RecordSpotViewServiceTest] で確かめる。
 * 同時に呼ばれた場合は [SpotViewConcurrencyTest] で確かめる。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SpotViewApiIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var spotJpa: SpotJpaRepository

    @Autowired
    private lateinit var countQuery: SpotViewCountQuery

    private fun saveSpot(id: String = SPOT_ID) {
        spotJpa.saveAndFlush(
            SpotEntity(id = id, genre = "landmark", latitude = 34.6, longitude = 135.1, imageUrl = "https://example.com/$id.jpg"),
        )
    }

    private fun viewCount(id: String = SPOT_ID): Long = countQuery.countBySpotId(SpotId.of(id))

    @Test
    fun `未ログインでも 204 で閲覧が 1 回数えられる`() {
        saveSpot()

        mockMvc.perform(post("/api/v1/tourism/spots/$SPOT_ID/views")).andExpect(status().isNoContent)

        assertEquals(1, viewCount())
    }

    @Test
    fun `ログイン済みの一般ユーザーでも 204 で数えられる`() {
        saveSpot()

        mockMvc
            .perform(post("/api/v1/tourism/spots/$SPOT_ID/views").with(withRole(Role.GENERAL)))
            .andExpect(status().isNoContent)

        assertEquals(1, viewCount())
    }

    @Test
    fun `期限切れなど無効なトークンを付けると 401 で数えない（公開 API でも付けたトークンは検証される）`() {
        saveSpot()

        mockMvc
            .perform(post("/api/v1/tourism/spots/$SPOT_ID/views").header("Authorization", "Bearer invalid-token"))
            .andExpect(status().isUnauthorized)

        assertEquals(0, viewCount())
    }

    @Test
    fun `呼ぶたびに閲覧数が増える`() {
        saveSpot()

        repeat(3) {
            mockMvc.perform(post("/api/v1/tourism/spots/$SPOT_ID/views")).andExpect(status().isNoContent)
        }

        assertEquals(3, viewCount())
    }

    @Test
    fun `ほかのスポットの閲覧数は増えない`() {
        saveSpot(SPOT_ID)
        saveSpot(OTHER_SPOT_ID)

        mockMvc.perform(post("/api/v1/tourism/spots/$SPOT_ID/views")).andExpect(status().isNoContent)

        assertEquals(0, viewCount(OTHER_SPOT_ID))
    }

    @Test
    fun `存在しないスポットは 404 で数えない`() {
        mockMvc.perform(post("/api/v1/tourism/spots/missing-spot/views")).andExpect(status().isNotFound)

        assertEquals(0, viewCount("missing-spot"))
    }

    @Test
    fun `spotId が 128 文字を超えると 400`() {
        mockMvc.perform(post("/api/v1/tourism/spots/${"a".repeat(129)}/views")).andExpect(status().isBadRequest)
    }

    @Test
    fun `GET は用意していない`() {
        saveSpot()

        mockMvc.perform(get("/api/v1/tourism/spots/$SPOT_ID/views")).andExpect(status().isMethodNotAllowed)
    }

    private companion object {
        const val SPOT_ID = "kobe-port-tower"
        const val OTHER_SPOT_ID = "kitano-ijinkan"
    }
}

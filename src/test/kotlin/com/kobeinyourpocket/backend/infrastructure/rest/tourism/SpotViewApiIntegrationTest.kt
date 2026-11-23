package com.kobeinyourpocket.backend.infrastructure.rest.tourism

import com.kobeinyourpocket.backend.application.tourism.query.SpotViewCountQuery
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId
import com.kobeinyourpocket.backend.domain.user.vo.Role
import com.kobeinyourpocket.backend.infrastructure.persistence.tourism.SpotEntity
import com.kobeinyourpocket.backend.infrastructure.persistence.tourism.SpotJpaRepository
import com.kobeinyourpocket.backend.infrastructure.ratelimit.ClientIps.fromIp
import com.kobeinyourpocket.backend.infrastructure.ratelimit.ClientIps.uniqueClientIp
import com.kobeinyourpocket.backend.infrastructure.security.withRole
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
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
 *
 * 重複まとめ・レート制限（#260）のカウンタはテストのロールバックで消えないため、閲覧を記録するときは
 * テストごとに別の IP（[uniqueClientIp]）から送る。10 分経ったら再び数えることは
 * [com.kobeinyourpocket.backend.infrastructure.ratelimit.InMemorySpotViewDeduplicatorTest] で確かめる。
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

    /** このテストの利用者 IP。テストごとに別にし、ほかのテストの閲覧とまとめられないようにする。 */
    private val ip = uniqueClientIp()

    private fun recordView(
        id: String = SPOT_ID,
        from: String = ip,
    ) = mockMvc.perform(post("/api/v1/tourism/spots/$id/views").with(fromIp(from)))

    @Test
    fun `未ログインでも 204 で閲覧が 1 回数えられる`() {
        saveSpot()

        recordView().andExpect(status().isNoContent)

        assertEquals(1, viewCount())
    }

    @Test
    fun `ログイン済みの一般ユーザーでも 204 で数えられる`() {
        saveSpot()

        mockMvc
            .perform(post("/api/v1/tourism/spots/$SPOT_ID/views").with(fromIp(ip)).with(withRole(Role.GENERAL)))
            .andExpect(status().isNoContent)

        assertEquals(1, viewCount())
    }

    @Test
    fun `期限切れなど無効なトークンを付けると 401 で数えない（公開 API でも付けたトークンは検証される）`() {
        saveSpot()

        mockMvc
            .perform(
                post("/api/v1/tourism/spots/$SPOT_ID/views").with(fromIp(ip)).header("Authorization", "Bearer invalid-token"),
            ).andExpect(status().isUnauthorized)

        assertEquals(0, viewCount())
    }

    @Test
    fun `同じ IP から同じスポットを 10 分以内に何度呼んでも 1 回だけ数え、いずれも 204`() {
        saveSpot()

        repeat(3) { recordView().andExpect(status().isNoContent) }

        assertEquals(1, viewCount())
    }

    @Test
    fun `別の IP からなら数える`() {
        saveSpot()

        recordView(from = ip).andExpect(status().isNoContent)
        recordView(from = uniqueClientIp()).andExpect(status().isNoContent)

        assertEquals(2, viewCount())
    }

    @Test
    fun `同じ IP からでも別のスポットなら数え、ほかのスポットの閲覧数は増えない`() {
        saveSpot(SPOT_ID)
        saveSpot(OTHER_SPOT_ID)
        saveSpot(THIRD_SPOT_ID)

        recordView(SPOT_ID).andExpect(status().isNoContent)
        recordView(OTHER_SPOT_ID).andExpect(status().isNoContent)

        assertEquals(1, viewCount(SPOT_ID))
        assertEquals(1, viewCount(OTHER_SPOT_ID))
        assertEquals(0, viewCount(THIRD_SPOT_ID))
    }

    @Test
    fun `存在しないスポットは 404 で数えない`() {
        recordView("missing-spot").andExpect(status().isNotFound)

        assertEquals(0, viewCount("missing-spot"))
    }

    @Test
    fun `404 はまとめる対象として数えず、あとでスポットができれば数える`() {
        recordView().andExpect(status().isNotFound)
        saveSpot()

        recordView().andExpect(status().isNoContent)

        assertEquals(1, viewCount())
    }

    @Test
    fun `同じ IP から 1 分に 60 回を超えると 429 と Retry-After を返す`() {
        saveSpot()

        repeat(60) { recordView().andExpect(status().isNoContent) }

        recordView()
            .andExpect(status().isTooManyRequests)
            .andExpect(header().exists("Retry-After"))
            .andExpect(jsonPath("$.status").value(429))
        // ほかの IP は止まらない
        recordView(from = uniqueClientIp()).andExpect(status().isNoContent)
        assertEquals(2, viewCount())
    }

    @Test
    fun `spotId が 128 文字を超えると 400`() {
        recordView("a".repeat(129)).andExpect(status().isBadRequest)
    }

    @Test
    fun `GET は用意していない`() {
        saveSpot()

        mockMvc.perform(get("/api/v1/tourism/spots/$SPOT_ID/views")).andExpect(status().isMethodNotAllowed)
    }

    private companion object {
        const val SPOT_ID = "kobe-port-tower"
        const val OTHER_SPOT_ID = "kitano-ijinkan"
        const val THIRD_SPOT_ID = "meriken-park"
    }
}

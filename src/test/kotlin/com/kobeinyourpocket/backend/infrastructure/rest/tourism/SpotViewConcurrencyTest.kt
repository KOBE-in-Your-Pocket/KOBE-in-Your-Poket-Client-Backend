package com.kobeinyourpocket.backend.infrastructure.rest.tourism

import com.kobeinyourpocket.backend.application.tourism.query.SpotViewCountQuery
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId
import com.kobeinyourpocket.backend.infrastructure.persistence.tourism.SpotEntity
import com.kobeinyourpocket.backend.infrastructure.persistence.tourism.SpotJpaRepository
import com.kobeinyourpocket.backend.infrastructure.persistence.tourism.SpotViewJpaRepository
import com.kobeinyourpocket.backend.infrastructure.ratelimit.ClientIps.fromIp
import com.kobeinyourpocket.backend.infrastructure.ratelimit.ClientIps.uniqueClientIp
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 閲覧を数える API を同時に呼んでも数がずれないこと（#240）。
 * 同じ IP から同時に呼んでも 1 回にまとまること（#260）。
 *
 * スレッドごとに別のトランザクションでコミットさせるため、テストクラスに @Transactional を付けない。
 * そのぶん、作ったデータはテストのあとに自分で消す。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SpotViewConcurrencyTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var spotJpa: SpotJpaRepository

    @Autowired
    private lateinit var spotViewJpa: SpotViewJpaRepository

    @Autowired
    private lateinit var countQuery: SpotViewCountQuery

    @AfterTest
    fun cleanUp() {
        spotViewJpa.deleteAll()
        spotJpa.deleteById(SPOT_ID)
    }

    /** [ips] の順に、同時に閲覧を記録する。戻り値は各リクエストの status。 */
    private fun recordConcurrently(ips: List<String>): List<Int> {
        spotJpa.saveAndFlush(
            SpotEntity(id = SPOT_ID, genre = "landmark", latitude = 34.6, longitude = 135.1, imageUrl = "https://example.com/spot.jpg"),
        )
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(THREADS)
        val statuses =
            ips.map { ip ->
                pool.submit<Int> {
                    start.await()
                    mockMvc
                        .perform(post("/api/v1/tourism/spots/$SPOT_ID/views").with(fromIp(ip)))
                        .andReturn()
                        .response.status
                }
            }

        start.countDown()
        val results = statuses.map { it.get(30, TimeUnit.SECONDS) }
        pool.shutdown()
        return results
    }

    @Test
    fun `別々の IP から同時に呼んでも呼んだ回数だけ数えられる`() {
        val results = recordConcurrently(List(REQUESTS) { uniqueClientIp() })

        assertEquals(List(REQUESTS) { 204 }, results)
        assertEquals(REQUESTS.toLong(), countQuery.countBySpotId(SpotId.of(SPOT_ID)))
    }

    @Test
    fun `同じ IP から同時に呼んでも 1 回だけ数えられる`() {
        val ip = uniqueClientIp()

        val results = recordConcurrently(List(REQUESTS) { ip })

        assertEquals(List(REQUESTS) { 204 }, results)
        assertEquals(1, countQuery.countBySpotId(SpotId.of(SPOT_ID)))
    }

    private companion object {
        const val SPOT_ID = "concurrent-spot"
        const val THREADS = 8
        const val REQUESTS = 40
    }
}

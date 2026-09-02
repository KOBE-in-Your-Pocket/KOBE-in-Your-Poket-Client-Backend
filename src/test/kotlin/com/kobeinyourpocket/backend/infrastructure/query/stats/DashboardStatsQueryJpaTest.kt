package com.kobeinyourpocket.backend.infrastructure.query.stats

import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.infrastructure.persistence.tourism.ReviewEntity
import com.kobeinyourpocket.backend.infrastructure.persistence.tourism.ReviewJpaRepository
import com.kobeinyourpocket.backend.infrastructure.persistence.tourism.SpotEntity
import com.kobeinyourpocket.backend.infrastructure.persistence.tourism.SpotJpaRepository
import com.kobeinyourpocket.backend.infrastructure.persistence.tourism.SpotLocalizationEntity
import com.kobeinyourpocket.backend.infrastructure.persistence.tourism.SpotLocalizationId
import com.kobeinyourpocket.backend.infrastructure.persistence.tourism.SpotLocalizationJpaRepository
import com.kobeinyourpocket.backend.infrastructure.persistence.user.UserEntity
import com.kobeinyourpocket.backend.infrastructure.persistence.user.UserJpaRepository
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * ダッシュボード集計 SQL の検証（#169）。
 *
 * 集計は SQL 側で行うため、境界（半開区間）・同数時の並び・スポット名のフォールバックは
 * 実 DB でしか確かめられない。
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(DashboardStatsQueryJpa::class)
class DashboardStatsQueryJpaTest {
    @Autowired
    private lateinit var userJpaRepository: UserJpaRepository

    @Autowired
    private lateinit var spotJpaRepository: SpotJpaRepository

    @Autowired
    private lateinit var spotLocalizationJpaRepository: SpotLocalizationJpaRepository

    @Autowired
    private lateinit var reviewJpaRepository: ReviewJpaRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var query: DashboardStatsQueryJpa

    private val august = Instant.parse("2026-08-15T00:00:00Z")
    private val september = Instant.parse("2026-09-15T00:00:00Z")

    private fun saveUser(
        name: String,
        createdAt: Instant,
    ) {
        userJpaRepository.save(
            UserEntity(
                id = UUID.randomUUID(),
                name = name,
                iconUrl = "",
                createdAt = createdAt,
                updatedAt = createdAt,
            ),
        )
    }

    /** spot.created_at は `@CreationTimestamp` で採番されるため、過去日時は保存後に上書きする。 */
    private fun saveSpot(
        id: String,
        names: Map<Language, String>,
        createdAt: Instant? = null,
    ) {
        spotJpaRepository.save(
            SpotEntity(
                id = id,
                genre = "landmark",
                latitude = 34.6,
                longitude = 135.1,
                imageUrl = "https://example.com/$id.jpg",
            ),
        )
        names.forEach { (language, name) ->
            spotLocalizationJpaRepository.save(
                SpotLocalizationEntity(
                    id = SpotLocalizationId(spotId = id, language = language.code),
                    name = name,
                    categoryLabel = "カテゴリ",
                    description = "説明",
                    businessHours = "24時間",
                    address = "神戸市",
                ),
            )
        }
        if (createdAt != null) {
            entityManager.flush()
            entityManager
                .createNativeQuery("UPDATE spot SET created_at = :createdAt WHERE id = :id")
                .setParameter("createdAt", createdAt.atOffset(ZoneOffset.UTC))
                .setParameter("id", id)
                .executeUpdate()
        }
    }

    private fun saveReview(
        spotId: String,
        authorName: String,
        createdAt: Instant,
        rating: Short = 5,
    ): UUID {
        val id = UUID.randomUUID()
        reviewJpaRepository.save(
            ReviewEntity(
                id = id,
                spotId = spotId,
                rating = rating,
                comment = "コメント",
                authorName = authorName,
                authorIconUrl = "",
                language = Language.JA.code,
                createdAt = createdAt,
            ),
        )
        return id
    }

    @Test
    fun `総数を 1 行で返す`() {
        saveUser("田中 美咲", august)
        saveUser("山田 花子", september)
        saveSpot("arima-onsen", mapOf(Language.JA to "有馬温泉"))
        saveReview("arima-onsen", "田中 美咲", september)

        val counts = query.countAll()

        assertEquals(2, counts.users)
        assertEquals(1, counts.spots)
        assertEquals(1, counts.reviews)
    }

    @Test
    fun `期間集計は from 以上 until 未満で数える`() {
        val from = Instant.parse("2026-09-01T00:00:00Z")
        val until = Instant.parse("2026-10-01T00:00:00Z")
        saveUser("境界前", from.minusMillis(1))
        saveUser("境界ちょうど", from)
        saveUser("期間内", september)
        saveUser("終端ちょうど", until)

        val counts = query.countCreatedIn(from, until)

        assertEquals(2, counts.users)
    }

    @Test
    fun `期間集計はスポットとレビューにも効く`() {
        val from = Instant.parse("2026-09-01T00:00:00Z")
        val until = Instant.parse("2026-10-01T00:00:00Z")
        saveSpot("in-range", mapOf(Language.JA to "期間内"), createdAt = september)
        saveSpot("out-of-range", mapOf(Language.JA to "期間外"), createdAt = august)
        saveReview("in-range", "田中 美咲", september)
        saveReview("out-of-range", "山田 花子", august)

        val counts = query.countCreatedIn(from, until)

        assertEquals(1, counts.spots)
        assertEquals(1, counts.reviews)
    }

    @Test
    fun `人気スポットはレビュー数の降順で返す`() {
        saveSpot("arima-onsen", mapOf(Language.JA to "有馬温泉"))
        saveSpot("kobe-port-tower", mapOf(Language.JA to "神戸ポートタワー"))
        saveReview("arima-onsen", "田中 美咲", september)
        saveReview("arima-onsen", "山田 花子", september)
        saveReview("kobe-port-tower", "中村 陽子", september)

        val popular = query.findPopularSpots(Language.JA, limit = 5)

        assertEquals(listOf("arima-onsen", "kobe-port-tower"), popular.map { it.spotId })
        assertEquals(listOf(2L, 1L), popular.map { it.reviewCount })
        assertEquals("有馬温泉", popular[0].name)
    }

    @Test
    fun `レビュー数が同じスポットは spot_id 順で安定させる`() {
        saveSpot("b-spot", mapOf(Language.JA to "B"))
        saveSpot("a-spot", mapOf(Language.JA to "A"))
        saveReview("b-spot", "田中 美咲", september)
        saveReview("a-spot", "山田 花子", september)

        val popular = query.findPopularSpots(Language.JA, limit = 5)

        assertEquals(listOf("a-spot", "b-spot"), popular.map { it.spotId })
    }

    @Test
    fun `人気スポットは limit 件までに絞る`() {
        repeat(3) { index ->
            val id = "spot-$index"
            saveSpot(id, mapOf(Language.JA to "スポット$index"))
            saveReview(id, "田中 美咲", september)
        }

        val popular = query.findPopularSpots(Language.JA, limit = 2)

        assertEquals(2, popular.size)
    }

    @Test
    fun `スポット名は要求言語で解決し、無ければ en へフォールバックする`() {
        saveSpot("arima-onsen", mapOf(Language.JA to "有馬温泉", Language.EN to "Arima Onsen"))
        saveSpot("ikuta-shrine", mapOf(Language.EN to "Ikuta Shrine"))
        saveReview("arima-onsen", "田中 美咲", september)
        saveReview("ikuta-shrine", "山田 花子", september)

        val popular = query.findPopularSpots(Language.JA, limit = 5).associateBy { it.spotId }

        assertEquals("有馬温泉", popular.getValue("arima-onsen").name)
        assertEquals("Ikuta Shrine", popular.getValue("ikuta-shrine").name)
    }

    @Test
    fun `ローカライズが 1 件も無いスポットは spot_id を名前として返す`() {
        saveSpot("no-localization", emptyMap())
        saveReview("no-localization", "田中 美咲", september)

        val popular = query.findPopularSpots(Language.JA, limit = 5)

        assertEquals("no-localization", popular.single().name)
    }

    @Test
    fun `直近レビューは新しい順に limit 件返す`() {
        saveSpot("arima-onsen", mapOf(Language.JA to "有馬温泉"))
        saveReview("arima-onsen", "古い", Instant.parse("2026-09-01T00:00:00Z"))
        saveReview("arima-onsen", "新しい", Instant.parse("2026-09-20T00:00:00Z"))
        saveReview("arima-onsen", "中間", Instant.parse("2026-09-10T00:00:00Z"))

        val recent = query.findRecentReviews(Language.JA, limit = 2)

        assertEquals(listOf("新しい", "中間"), recent.map { it.authorName })
        assertEquals("有馬温泉", recent[0].spotName)
        assertEquals(Instant.parse("2026-09-20T00:00:00Z"), recent[0].postedAt)
    }

    @Test
    fun `直近レビューは評価と投稿言語も返す`() {
        saveSpot("arima-onsen", mapOf(Language.JA to "有馬温泉"))
        val id = saveReview("arima-onsen", "田中 美咲", september, rating = 4)

        val review = query.findRecentReviews(Language.JA, limit = 5).single()

        assertEquals(id.toString(), review.id)
        assertEquals(4, review.rating)
        assertEquals(Language.JA.code, review.language)
    }

    @Test
    fun `レビューが無ければ人気スポットも直近レビューも空になる`() {
        saveSpot("arima-onsen", mapOf(Language.JA to "有馬温泉"))

        assertEquals(emptyList(), query.findPopularSpots(Language.JA, limit = 5))
        assertEquals(emptyList(), query.findRecentReviews(Language.JA, limit = 5))
    }
}

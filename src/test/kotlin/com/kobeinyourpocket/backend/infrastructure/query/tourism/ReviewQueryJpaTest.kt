package com.kobeinyourpocket.backend.infrastructure.query.tourism

import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId
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
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * レビュー一覧の投稿者アイコン解決の検証（#208 / #209）。
 *
 * 投稿者アイコンは `review.author_icon_url`（投稿時点のスナップショット）ではなく
 * `users.icon_url`（現在のプロフィール）から解決する。結合と CASE は SQL 側で行うため実 DB で確かめる。
 * スポット別一覧（[ReviewQueryJpa.findBySpot]）と運営向け一覧（[ReviewQueryJpa.findPage]）の両方を見る。
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(ReviewQueryJpa::class)
class ReviewQueryJpaTest {
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
    private lateinit var query: ReviewQueryJpa

    private val spotId = "kobe-port-tower"
    private val postedAt = Instant.parse("2026-09-15T00:00:00Z")

    private fun saveSpot() {
        spotJpaRepository.save(
            SpotEntity(
                id = spotId,
                genre = "landmark",
                latitude = 34.6,
                longitude = 135.1,
                imageUrl = "https://example.com/$spotId.jpg",
            ),
        )
        spotLocalizationJpaRepository.save(
            SpotLocalizationEntity(
                id = SpotLocalizationId(spotId = spotId, language = Language.EN.code),
                name = "Kobe Port Tower",
                categoryLabel = "Landmark",
                description = "The symbol of Kobe.",
                businessHours = "24h",
                address = "Kobe",
            ),
        )
    }

    private fun saveUser(iconUrl: String): UUID {
        val id = UUID.randomUUID()
        userJpaRepository.save(
            UserEntity(id = id, name = "Alice", iconUrl = iconUrl, createdAt = postedAt, updatedAt = postedAt),
        )
        return id
    }

    private fun saveReview(
        authorUserId: UUID?,
        authorIconUrl: String,
    ) {
        reviewJpaRepository.save(
            ReviewEntity(
                id = UUID.randomUUID(),
                spotId = spotId,
                rating = 5,
                comment = "最高の景色",
                authorName = "Alice",
                authorIconUrl = authorIconUrl,
                authorUserId = authorUserId,
                language = Language.JA.code,
                createdAt = postedAt,
            ),
        )
        entityManager.flush()
    }

    /** 同じレビューを両方の一覧から読み、投稿者アイコンを返す（両経路の食い違いもここで検出する）。 */
    private fun authorIconUrlOfOnlyReview(): String? {
        val bySpot = query.findBySpot(SpotId.of(spotId), Language.JA).single().authorIconUrl
        val page =
            query
                .findPage(page = 0, size = 10, language = Language.JA)
                .reviews
                .single()
                .authorIconUrl
        assertEquals(bySpot, page, "スポット別一覧と運営向け一覧で投稿者アイコンが食い違う")
        return bySpot
    }

    @Test
    fun `投稿者アイコンは投稿時点の値ではなく現在のプロフィールから返す`() {
        saveSpot()
        val userId = saveUser(iconUrl = "https://example.com/new.webp")
        saveReview(authorUserId = userId, authorIconUrl = "https://example.com/old.webp")

        assertEquals("https://example.com/new.webp", authorIconUrlOfOnlyReview())
    }

    @Test
    fun `投稿後にアイコンを変更すると既存レビューにも反映される`() {
        saveSpot()
        val userId = saveUser(iconUrl = "")
        saveReview(authorUserId = userId, authorIconUrl = "")

        entityManager
            .createNativeQuery("UPDATE users SET icon_url = :iconUrl WHERE id = :id")
            .setParameter("iconUrl", "https://example.com/changed.webp")
            .setParameter("id", userId)
            .executeUpdate()

        assertEquals("https://example.com/changed.webp", authorIconUrlOfOnlyReview())
    }

    @Test
    fun `アイコンを未設定に戻した投稿者は投稿時点のアイコンがあっても null`() {
        // 空文字を「未設定」とみなしてスナップショットへフォールバックすると、外したはずの旧アイコンが復活する。
        saveSpot()
        val userId = saveUser(iconUrl = "")
        saveReview(authorUserId = userId, authorIconUrl = "https://example.com/old.webp")

        assertNull(authorIconUrlOfOnlyReview())
    }

    @Test
    fun `投稿者不明の旧レビューは投稿時点のアイコンを返す`() {
        // V17 以前の投稿は author_user_id が NULL で、プロフィールと結び付けられない。
        saveSpot()
        saveReview(authorUserId = null, authorIconUrl = "https://example.com/legacy.webp")

        assertEquals("https://example.com/legacy.webp", authorIconUrlOfOnlyReview())
    }

    @Test
    fun `プロフィール行が無い投稿者のレビューも一覧から消えず投稿時点のアイコンを返す`() {
        saveSpot()
        saveReview(authorUserId = UUID.randomUUID(), authorIconUrl = "https://example.com/snapshot.webp")

        assertEquals("https://example.com/snapshot.webp", authorIconUrlOfOnlyReview())
    }

    @Test
    fun `プロフィール行が無く投稿時点のアイコンも空なら null`() {
        saveSpot()
        saveReview(authorUserId = UUID.randomUUID(), authorIconUrl = "")

        assertNull(authorIconUrlOfOnlyReview())
    }

    @Test
    fun `投稿者アイコンの結合で件数は変わらない`() {
        saveSpot()
        val userId = saveUser(iconUrl = "https://example.com/a.webp")
        saveReview(authorUserId = userId, authorIconUrl = "")
        saveReview(authorUserId = userId, authorIconUrl = "")
        saveReview(authorUserId = null, authorIconUrl = "")

        assertEquals(3, query.findBySpot(SpotId.of(spotId), Language.JA).size)
        val page = query.findPage(page = 0, size = 10, language = Language.JA)
        assertEquals(3, page.reviews.size)
        assertEquals(3, page.totalElements)
    }
}

package com.kobeinyourpocket.backend.infrastructure.query.tourism

import com.kobeinyourpocket.backend.application.tourism.query.FavoriteView
import com.kobeinyourpocket.backend.domain.tourism.favorite.vo.FavoriteOwnerId
import com.kobeinyourpocket.backend.infrastructure.persistence.tourism.FavoriteEntity
import com.kobeinyourpocket.backend.infrastructure.persistence.tourism.FavoriteJpaRepository
import com.kobeinyourpocket.backend.infrastructure.persistence.tourism.FavoriteKey
import com.kobeinyourpocket.backend.infrastructure.persistence.tourism.SpotEntity
import com.kobeinyourpocket.backend.infrastructure.persistence.tourism.SpotJpaRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

/** お気に入り一覧のクエリ（#237）。本人の分だけ・新しい順・削除済みスポットの除外を実 DB で確かめる。 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(FavoriteQueryJpa::class)
class FavoriteQueryJpaTest {
    @Autowired
    private lateinit var spotJpa: SpotJpaRepository

    @Autowired
    private lateinit var favoriteJpa: FavoriteJpaRepository

    @Autowired
    private lateinit var query: FavoriteQueryJpa

    private val alice = FavoriteOwnerId.of("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
    private val bob = FavoriteOwnerId.of("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")

    private fun saveSpot(id: String) {
        spotJpa.save(
            SpotEntity(id = id, genre = "landmark", latitude = 34.6, longitude = 135.1, imageUrl = "https://example.com/$id.jpg"),
        )
    }

    private fun saveFavorite(
        ownerId: FavoriteOwnerId,
        spotId: String,
        createdAt: String,
    ) {
        favoriteJpa.save(
            FavoriteEntity(
                id = FavoriteKey(userId = ownerId.value, spotId = spotId),
                createdAt = Instant.parse(createdAt),
            ),
        )
    }

    @Test
    fun `本人のお気に入りを登録日時の新しい順に返す`() {
        listOf("harborland", "kitano", "port-tower").forEach(::saveSpot)
        saveFavorite(alice, "kitano", "2026-11-01T10:00:00Z")
        saveFavorite(alice, "port-tower", "2026-11-03T10:00:00Z")
        saveFavorite(alice, "harborland", "2026-11-02T10:00:00Z")

        assertEquals(
            listOf(
                FavoriteView("port-tower", Instant.parse("2026-11-03T10:00:00Z")),
                FavoriteView("harborland", Instant.parse("2026-11-02T10:00:00Z")),
                FavoriteView("kitano", Instant.parse("2026-11-01T10:00:00Z")),
            ),
            query.findByOwner(alice),
        )
    }

    @Test
    fun `ほかの人のお気に入りは含めない`() {
        listOf("harborland", "kitano").forEach(::saveSpot)
        saveFavorite(alice, "harborland", "2026-11-01T10:00:00Z")
        saveFavorite(bob, "kitano", "2026-11-02T10:00:00Z")

        assertEquals(listOf("harborland"), query.findByOwner(alice).map { it.spotId })
    }

    @Test
    fun `登録日時が同じなら spotId の順に並べる`() {
        listOf("b-spot", "a-spot").forEach(::saveSpot)
        saveFavorite(alice, "b-spot", "2026-11-01T10:00:00Z")
        saveFavorite(alice, "a-spot", "2026-11-01T10:00:00Z")

        assertEquals(listOf("a-spot", "b-spot"), query.findByOwner(alice).map { it.spotId })
    }

    @Test
    fun `スポットが無いお気に入りは含めない`() {
        // 本番は外部キーで一緒に消えるが、外部キーの無い H2 ではお気に入りの行だけ残る。
        saveSpot("harborland")
        saveFavorite(alice, "harborland", "2026-11-01T10:00:00Z")
        saveFavorite(alice, "deleted-spot", "2026-11-02T10:00:00Z")

        assertEquals(listOf("harborland"), query.findByOwner(alice).map { it.spotId })
    }

    @Test
    fun `お気に入りが無ければ空のリスト`() {
        assertEquals(emptyList(), query.findByOwner(alice))
    }
}

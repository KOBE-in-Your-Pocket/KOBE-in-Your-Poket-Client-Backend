package com.kobeinyourpocket.backend.infrastructure.persistence.tourism

import com.kobeinyourpocket.backend.domain.tourism.favorite.model.Favorite
import com.kobeinyourpocket.backend.domain.tourism.favorite.repository.FavoriteRepository
import com.kobeinyourpocket.backend.domain.tourism.favorite.vo.FavoriteOwnerId
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * スポットへの外部キー（V25 の `fk_favorites_spot`）があるときの [FavoriteRepositoryImpl.saveIfAbsent]。
 *
 * テストの H2 は Flyway を使わずエンティティからテーブルを作るため、外部キーが付かない。
 * ここでは V25 と同じ外部キーを手で足して、外部キー違反を成功扱いにしないことを確かめる。
 *
 * 本番と同じく、保存をテストのトランザクションの外で動かす（NOT_SUPPORTED）。テストの
 * トランザクションの中だと、制約違反のあとにセッションが使えなくなり、確かめ直しができないため。
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(FavoriteRepositoryImpl::class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class FavoriteRepositoryImplForeignKeyTest {
    @Autowired
    private lateinit var repository: FavoriteRepository

    @Autowired
    private lateinit var favoriteJpa: FavoriteJpaRepository

    @Autowired
    private lateinit var jdbc: JdbcTemplate

    private val alice = FavoriteOwnerId.of("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
    private val harborland = SpotId.of("kobe-harborland")
    private val now = Instant.parse("2026-11-01T10:00:00Z")

    @BeforeEach
    fun addForeignKey() {
        jdbc.execute(
            "ALTER TABLE favorites ADD CONSTRAINT fk_favorites_spot " +
                "FOREIGN KEY (spot_id) REFERENCES spot (id) ON DELETE CASCADE",
        )
    }

    @AfterEach
    fun cleanUp() {
        jdbc.execute("DELETE FROM favorites")
        jdbc.execute("DELETE FROM spot")
        jdbc.execute("ALTER TABLE favorites DROP CONSTRAINT fk_favorites_spot")
    }

    @Test
    fun `存在するスポットなら登録できる`() {
        insertSpot(harborland)

        repository.saveIfAbsent(Favorite.create(ownerId = alice, spotId = harborland, createdAt = now))

        assertEquals(listOf(Favorite(alice, harborland, now)), favoriteJpa.findAll().map { it.toDomain() })
    }

    @Test
    fun `存在しないスポットは成功扱いにせず例外を投げて保存しない`() {
        assertFailsWith<DataIntegrityViolationException> {
            repository.saveIfAbsent(Favorite.create(ownerId = alice, spotId = harborland, createdAt = now))
        }

        assertTrue(favoriteJpa.findAll().isEmpty())
    }

    private fun insertSpot(id: SpotId) {
        jdbc.update(
            "INSERT INTO spot (id, genre, latitude, longitude, image_url, created_at, updated_at) " +
                "VALUES (?, 'sightseeing', 34.68, 135.18, '', ?, ?)",
            id.value,
            java.sql.Timestamp.from(now),
            java.sql.Timestamp.from(now),
        )
    }
}

package com.kobeinyourpocket.backend.infrastructure.persistence.tourism

import com.kobeinyourpocket.backend.domain.tourism.favorite.model.Favorite
import com.kobeinyourpocket.backend.domain.tourism.favorite.repository.FavoriteRepository
import com.kobeinyourpocket.backend.domain.tourism.favorite.vo.FavoriteOwnerId
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(FavoriteRepositoryImpl::class)
class FavoriteRepositoryImplTest {
    @Autowired
    private lateinit var repository: FavoriteRepository

    @Autowired
    private lateinit var favoriteJpa: FavoriteJpaRepository

    private val alice = FavoriteOwnerId.of("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
    private val bob = FavoriteOwnerId.of("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")
    private val harborland = SpotId.of("kobe-harborland")
    private val kitano = SpotId.of("kitano-ijinkan")
    private val firstAt = Instant.parse("2026-11-01T10:00:00Z")
    private val laterAt = Instant.parse("2026-11-02T10:00:00Z")

    private fun favorite(
        ownerId: FavoriteOwnerId = alice,
        spotId: SpotId = harborland,
        createdAt: Instant = firstAt,
    ) = Favorite.create(ownerId = ownerId, spotId = spotId, createdAt = createdAt)

    private fun stored(): List<Favorite> = favoriteJpa.findAll().map { it.toDomain() }

    @Test
    fun `保存したお気に入りを同じ内容で復元できる`() {
        repository.saveIfAbsent(favorite())

        assertEquals(listOf(favorite()), stored())
    }

    @Test
    fun `同じ人が同じスポットを 2 回登録しても 1 件のままで最初の登録日時が残る`() {
        repository.saveIfAbsent(favorite(createdAt = firstAt))
        repository.saveIfAbsent(favorite(createdAt = laterAt))

        assertEquals(listOf(favorite(createdAt = firstAt)), stored())
    }

    @Test
    fun `別の人や別のスポットなら登録できる`() {
        repository.saveIfAbsent(favorite(ownerId = alice, spotId = harborland))
        repository.saveIfAbsent(favorite(ownerId = bob, spotId = harborland))
        repository.saveIfAbsent(favorite(ownerId = alice, spotId = kitano))

        assertEquals(
            setOf(
                favorite(ownerId = alice, spotId = harborland),
                favorite(ownerId = bob, spotId = harborland),
                favorite(ownerId = alice, spotId = kitano),
            ),
            stored().toSet(),
        )
    }

    @Test
    fun `削除すると該当の組だけ消えてほかは残る`() {
        repository.saveIfAbsent(favorite(ownerId = alice, spotId = harborland))
        repository.saveIfAbsent(favorite(ownerId = bob, spotId = harborland))
        repository.saveIfAbsent(favorite(ownerId = alice, spotId = kitano))

        repository.delete(alice, harborland)

        assertEquals(
            setOf(
                favorite(ownerId = bob, spotId = harborland),
                favorite(ownerId = alice, spotId = kitano),
            ),
            stored().toSet(),
        )
    }

    @Test
    fun `登録していない組を削除しても例外にならない`() {
        repository.saveIfAbsent(favorite(ownerId = alice, spotId = harborland))

        repository.delete(alice, kitano)

        assertEquals(listOf(favorite(ownerId = alice, spotId = harborland)), stored())
    }
}

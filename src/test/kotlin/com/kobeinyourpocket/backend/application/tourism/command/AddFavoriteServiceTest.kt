package com.kobeinyourpocket.backend.application.tourism.command

import com.kobeinyourpocket.backend.application.tourism.SpotNotFoundException
import com.kobeinyourpocket.backend.domain.tourism.favorite.model.Favorite
import com.kobeinyourpocket.backend.domain.tourism.favorite.repository.FavoriteRepository
import com.kobeinyourpocket.backend.domain.tourism.favorite.vo.FavoriteOwnerId
import com.kobeinyourpocket.backend.domain.tourism.spot.repository.SpotRepository
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.springframework.dao.DataIntegrityViolationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AddFavoriteServiceTest {
    private val spotRepository = mockk<SpotRepository>()
    private val favoriteRepository = mockk<FavoriteRepository>()
    private val service = AddFavoriteService(spotRepository, favoriteRepository)

    private val alice = FavoriteOwnerId.of("11111111-1111-1111-1111-111111111111")
    private val harborland = SpotId.of("kobe-harborland")

    @Test
    fun `存在するスポットなら本人とスポットの組で保存する`() {
        every { spotRepository.existsById(harborland) } returns true
        val saved = slot<Favorite>()
        justRun { favoriteRepository.saveIfAbsent(capture(saved)) }

        service.execute(ownerId = alice, spotId = harborland)

        assertEquals(alice, saved.captured.ownerId)
        assertEquals(harborland, saved.captured.spotId)
    }

    @Test
    fun `存在しないスポットなら SpotNotFoundException で保存しない`() {
        every { spotRepository.existsById(harborland) } returns false

        assertFailsWith<SpotNotFoundException> {
            service.execute(ownerId = alice, spotId = harborland)
        }

        verify(exactly = 0) { favoriteRepository.saveIfAbsent(any()) }
    }

    @Test
    fun `存在確認のあとにスポットが削除されて外部キー違反になったら SpotNotFoundException`() {
        every { spotRepository.existsById(harborland) } returns true
        every { favoriteRepository.saveIfAbsent(any()) } throws DataIntegrityViolationException("fk_favorites_spot")

        assertFailsWith<SpotNotFoundException> {
            service.execute(ownerId = alice, spotId = harborland)
        }
    }
}

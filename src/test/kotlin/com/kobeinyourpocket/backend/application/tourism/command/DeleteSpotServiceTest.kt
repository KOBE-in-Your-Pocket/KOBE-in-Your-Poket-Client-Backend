package com.kobeinyourpocket.backend.application.tourism.command

import com.kobeinyourpocket.backend.application.tourism.SpotNotFoundException
import com.kobeinyourpocket.backend.domain.tourism.spot.repository.SpotRepository
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertFailsWith

class DeleteSpotServiceTest {
    private val spotRepository = mockk<SpotRepository>()
    private val service = DeleteSpotService(spotRepository)

    private val spotId = SpotId.of("kobe-port-tower")

    @Test
    fun `存在するスポットを削除する`() {
        every { spotRepository.existsById(spotId) } returns true
        justRun { spotRepository.deleteById(spotId) }

        service.execute(spotId)

        verify { spotRepository.deleteById(spotId) }
    }

    @Test
    fun `存在しないスポット ID を指定すると SpotNotFoundException`() {
        every { spotRepository.existsById(spotId) } returns false

        assertFailsWith<SpotNotFoundException> {
            service.execute(spotId)
        }

        verify(exactly = 0) { spotRepository.deleteById(any()) }
    }
}

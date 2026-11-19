package com.kobeinyourpocket.backend.application.tourism.command

import com.kobeinyourpocket.backend.application.tourism.SpotNotFoundException
import com.kobeinyourpocket.backend.domain.tourism.spot.repository.SpotRepository
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId
import com.kobeinyourpocket.backend.domain.tourism.view.model.SpotView
import com.kobeinyourpocket.backend.domain.tourism.view.repository.SpotViewRepository
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.springframework.dao.DataIntegrityViolationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RecordSpotViewServiceTest {
    private val spotRepository = mockk<SpotRepository>()
    private val spotViewRepository = mockk<SpotViewRepository>()
    private val service = RecordSpotViewService(spotRepository, spotViewRepository)

    private val harborland = SpotId.of("kobe-harborland")

    @Test
    fun `存在するスポットなら閲覧を 1 件記録する`() {
        every { spotRepository.existsById(harborland) } returns true
        val recorded = slot<SpotView>()
        justRun { spotViewRepository.record(capture(recorded)) }

        service.execute(harborland)

        assertEquals(harborland, recorded.captured.spotId)
        verify(exactly = 1) { spotViewRepository.record(any()) }
    }

    @Test
    fun `存在しないスポットなら SpotNotFoundException で記録しない`() {
        every { spotRepository.existsById(harborland) } returns false

        assertFailsWith<SpotNotFoundException> { service.execute(harborland) }

        verify(exactly = 0) { spotViewRepository.record(any()) }
    }

    @Test
    fun `存在確認のあとにスポットが削除されて外部キー違反になったら SpotNotFoundException`() {
        every { spotRepository.existsById(harborland) } returns true
        every { spotViewRepository.record(any()) } throws DataIntegrityViolationException("fk_spot_views_spot")

        assertFailsWith<SpotNotFoundException> { service.execute(harborland) }
    }
}

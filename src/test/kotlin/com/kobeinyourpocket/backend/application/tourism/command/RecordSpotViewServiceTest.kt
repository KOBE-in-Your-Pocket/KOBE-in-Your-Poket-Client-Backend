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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RecordSpotViewServiceTest {
    private val spotRepository = mockk<SpotRepository>()
    private val spotViewRepository = mockk<SpotViewRepository>()
    private val deduplicator = mockk<SpotViewDeduplicator>()
    private val service = RecordSpotViewService(spotRepository, spotViewRepository, deduplicator)

    private val harborland = SpotId.of("kobe-harborland")
    private val viewer = "203.0.113.1"

    @Test
    fun `存在するスポットなら閲覧を 1 件記録する`() {
        every { spotRepository.existsById(harborland) } returns true
        every { deduplicator.tryCount(viewer, harborland) } returns true
        val recorded = slot<SpotView>()
        justRun { spotViewRepository.record(capture(recorded)) }

        assertTrue(service.execute(harborland, viewer))

        assertEquals(harborland, recorded.captured.spotId)
        verify(exactly = 1) { spotViewRepository.record(any()) }
    }

    @Test
    fun `同じ閲覧者の一定時間内の重複なら記録せず false`() {
        every { spotRepository.existsById(harborland) } returns true
        every { deduplicator.tryCount(viewer, harborland) } returns false

        assertFalse(service.execute(harborland, viewer))

        verify(exactly = 0) { spotViewRepository.record(any()) }
    }

    @Test
    fun `存在しないスポットなら SpotNotFoundException で記録しない`() {
        every { spotRepository.existsById(harborland) } returns false

        assertFailsWith<SpotNotFoundException> { service.execute(harborland, viewer) }

        verify(exactly = 0) { spotViewRepository.record(any()) }
        // 存在しないスポットへの呼び出しで「数えた」扱いを消費しない
        verify(exactly = 0) { deduplicator.tryCount(any(), any()) }
    }

    @Test
    fun `存在確認のあとにスポットが削除されて外部キー違反になったら SpotNotFoundException`() {
        every { spotRepository.existsById(harborland) } returns true
        every { deduplicator.tryCount(viewer, harborland) } returns true
        every { spotViewRepository.record(any()) } throws DataIntegrityViolationException("fk_spot_views_spot")

        assertFailsWith<SpotNotFoundException> { service.execute(harborland, viewer) }
    }
}

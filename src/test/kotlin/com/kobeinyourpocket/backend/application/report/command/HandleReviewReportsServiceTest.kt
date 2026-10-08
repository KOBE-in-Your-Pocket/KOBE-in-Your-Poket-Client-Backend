package com.kobeinyourpocket.backend.application.report.command

import com.kobeinyourpocket.backend.application.report.ReportsNotFoundException
import com.kobeinyourpocket.backend.domain.report.model.Report
import com.kobeinyourpocket.backend.domain.report.repository.ReportRepository
import com.kobeinyourpocket.backend.domain.report.vo.ReportHandlerId
import com.kobeinyourpocket.backend.domain.report.vo.ReportReason
import com.kobeinyourpocket.backend.domain.report.vo.ReportStatus
import com.kobeinyourpocket.backend.domain.report.vo.ReportTarget
import com.kobeinyourpocket.backend.domain.report.vo.ReporterId
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewId
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HandleReviewReportsServiceTest {
    private val repository = mockk<ReportRepository>()
    private val service = HandleReviewReportsService(repository)

    private val reviewId = ReviewId.of("00000000-0000-0000-0000-000000000001")
    private val target = ReportTarget(ReportTarget.Type.REVIEW, reviewId.value.toString())
    private val operator = ReportHandlerId.of("33333333-3333-3333-3333-333333333333")
    private val now = Instant.parse("2026-10-07T03:00:00Z")

    private fun openReport(reporter: String) =
        Report.create(
            target = target,
            reporterId = ReporterId.of(reporter),
            reason = ReportReason.SPAM,
            description = null,
        )

    @Test
    fun `口コミへの未対応の通報をまとめて閉じ、件数を返す`() {
        val open = listOf(openReport("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"), openReport("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"))
        every { repository.existsByTarget(target) } returns true
        every { repository.findOpenByTarget(target) } returns open
        val saved = slot<List<Report>>()
        justRun { repository.saveAll(capture(saved)) }

        val updated = service.execute(reviewId, ReportStatus.DISMISSED, operator, now)

        assertEquals(2, updated)
        assertEquals(listOf(ReportStatus.DISMISSED), saved.captured.map(Report::status).distinct())
        assertEquals(listOf(operator), saved.captured.map(Report::handledBy).distinct())
    }

    @Test
    fun `全件対応済みなら 0 件で成功する（冪等）`() {
        every { repository.existsByTarget(target) } returns true
        every { repository.findOpenByTarget(target) } returns emptyList()

        assertEquals(0, service.execute(reviewId, ReportStatus.APPROVED, operator, now))

        verify(exactly = 0) { repository.saveAll(any()) }
    }

    @Test
    fun `通報が 1 件も無い口コミは ReportsNotFoundException`() {
        every { repository.existsByTarget(target) } returns false

        assertFailsWith<ReportsNotFoundException> { service.execute(reviewId, ReportStatus.APPROVED, operator, now) }
    }

    @Test
    fun `口コミ削除時は未対応の通報を対応済みにし、通報が無くても例外にしない`() {
        every { repository.findOpenByTarget(target) } returns emptyList()

        assertEquals(0, service.approveOnReviewDeleted(reviewId, operator, now))

        verify(exactly = 0) { repository.existsByTarget(any()) }
    }
}

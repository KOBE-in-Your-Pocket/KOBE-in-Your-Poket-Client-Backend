package com.kobeinyourpocket.backend.application.report.query

import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.domain.report.vo.ReportStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test

class ListReviewReportsServiceTest {
    private val query = mockk<ReviewReportQuery>()
    private val service = ListReviewReportsService(query)
    private val empty = ReviewReportPageView(groups = emptyList(), page = 0, size = 1, totalElements = 0)

    init {
        every { query.findPage(any(), any(), any(), any()) } returns empty
    }

    @Test
    fun `未指定なら先頭ページを既定の件数で返し、対応状況で絞らない`() {
        service.listReviewReports(Language.JA)

        verify { query.findPage(null, 0, ListReviewReportsService.DEFAULT_SIZE, Language.JA) }
    }

    @Test
    fun `負のページは先頭に、件数は 1 から上限までに丸める`() {
        service.listReviewReports(Language.JA, ReportStatus.OPEN, page = -3, size = 10_000)
        service.listReviewReports(Language.JA, ReportStatus.OPEN, page = 2, size = 0)

        verify { query.findPage(ReportStatus.OPEN, 0, ListReviewReportsService.MAX_SIZE, Language.JA) }
        verify { query.findPage(ReportStatus.OPEN, 2, 1, Language.JA) }
    }

    @Test
    fun `上限は他の運営向け一覧と同じ 200 件`() {
        service.listReviewReports(Language.JA, size = 200)
        service.listReviewReports(Language.JA, size = 201)

        verify(exactly = 2) { query.findPage(null, 0, 200, Language.JA) }
    }
}

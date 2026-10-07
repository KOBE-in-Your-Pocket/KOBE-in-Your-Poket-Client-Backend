package com.kobeinyourpocket.backend.application.report.command

import com.kobeinyourpocket.backend.application.report.AlreadyReportedException
import com.kobeinyourpocket.backend.application.report.CannotReportOwnContentException
import com.kobeinyourpocket.backend.application.tourism.ReviewNotFoundException
import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.domain.report.model.Report
import com.kobeinyourpocket.backend.domain.report.repository.ReportRepository
import com.kobeinyourpocket.backend.domain.report.vo.ReportReason
import com.kobeinyourpocket.backend.domain.report.vo.ReportTarget
import com.kobeinyourpocket.backend.domain.report.vo.ReporterId
import com.kobeinyourpocket.backend.domain.tourism.review.model.Review
import com.kobeinyourpocket.backend.domain.tourism.review.repository.ReviewRepository
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewAuthor
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewAuthorId
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewId
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewRating
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ReportReviewServiceTest {
    private val reviewRepository = mockk<ReviewRepository>()
    private val reportRepository = mockk<ReportRepository>()
    private val service = ReportReviewService(reviewRepository, reportRepository)

    private val spotId = SpotId.of("kobe-port-tower")
    private val reviewId = ReviewId.of("00000000-0000-0000-0000-000000000001")
    private val authorUuid = "11111111-1111-1111-1111-111111111111"
    private val reporter = ReporterId.of("22222222-2222-2222-2222-222222222222")

    private val review =
        Review(
            id = reviewId,
            spotId = spotId,
            rating = ReviewRating.of(1),
            comment = "宣伝です",
            author = ReviewAuthor(name = "Alice", userId = ReviewAuthorId.of(authorUuid)),
            createdAt = Instant.parse("2025-11-03T10:00:00Z"),
            language = Language.JA,
        )

    private fun report(
        reporterId: ReporterId = reporter,
        reason: ReportReason = ReportReason.SPAM,
        description: String? = null,
        spot: SpotId = spotId,
    ) = service.execute(spot, reviewId, reporterId, reason, description)

    @Test
    fun `他人のレビューを通報すると REVIEW を対象に保存する`() {
        every { reviewRepository.findById(reviewId) } returns review
        val saved = slot<Report>()
        every { reportRepository.saveIfNotReported(capture(saved)) } returns true

        val result = report(reason = ReportReason.OTHER, description = "営業時間が嘘")

        assertEquals(ReportTarget(ReportTarget.Type.REVIEW, reviewId.value.toString()), saved.captured.target)
        assertEquals(reporter, saved.captured.reporterId)
        assertEquals(ReportReason.OTHER, saved.captured.reason)
        assertEquals("営業時間が嘘", saved.captured.description)
        assertEquals(saved.captured, result)
    }

    @Test
    fun `存在しないレビューは ReviewNotFoundException`() {
        every { reviewRepository.findById(reviewId) } returns null

        assertFailsWith<ReviewNotFoundException> { report() }

        verify(exactly = 0) { reportRepository.saveIfNotReported(any()) }
    }

    @Test
    fun `パスのスポット配下にないレビューは ReviewNotFoundException`() {
        every { reviewRepository.findById(reviewId) } returns review

        assertFailsWith<ReviewNotFoundException> { report(spot = SpotId.of("other-spot")) }

        verify(exactly = 0) { reportRepository.saveIfNotReported(any()) }
    }

    @Test
    fun `自分のレビューは通報できない`() {
        every { reviewRepository.findById(reviewId) } returns review

        assertFailsWith<CannotReportOwnContentException> { report(reporterId = ReporterId.of(authorUuid)) }

        verify(exactly = 0) { reportRepository.saveIfNotReported(any()) }
    }

    @Test
    fun `投稿者不明の古いレビューも通報できる`() {
        // V17 以前の投稿。本人判定ができないが、通報は誰でもできる。
        every { reviewRepository.findById(reviewId) } returns review.copy(author = ReviewAuthor(name = "Alice"))
        every { reportRepository.saveIfNotReported(any()) } returns true

        report()

        verify(exactly = 1) { reportRepository.saveIfNotReported(any()) }
    }

    @Test
    fun `同じレビューをすでに通報していたら AlreadyReportedException`() {
        every { reviewRepository.findById(reviewId) } returns review
        every { reportRepository.saveIfNotReported(any()) } returns false

        assertFailsWith<AlreadyReportedException> { report() }
    }

    @Test
    fun `その他で詳細が無ければ保存せず IllegalArgumentException`() {
        every { reviewRepository.findById(reviewId) } returns review

        assertFailsWith<IllegalArgumentException> { report(reason = ReportReason.OTHER, description = null) }

        verify(exactly = 0) { reportRepository.saveIfNotReported(any()) }
    }
}

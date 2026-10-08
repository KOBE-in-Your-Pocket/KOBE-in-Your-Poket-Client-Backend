package com.kobeinyourpocket.backend.infrastructure.persistence.report

import com.kobeinyourpocket.backend.domain.report.model.Report
import com.kobeinyourpocket.backend.domain.report.repository.ReportRepository
import com.kobeinyourpocket.backend.domain.report.vo.ReportHandlerId
import com.kobeinyourpocket.backend.domain.report.vo.ReportReason
import com.kobeinyourpocket.backend.domain.report.vo.ReportStatus
import com.kobeinyourpocket.backend.domain.report.vo.ReportTarget
import com.kobeinyourpocket.backend.domain.report.vo.ReporterId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(ReportRepositoryImpl::class)
class ReportRepositoryImplTest {
    @Autowired
    private lateinit var repository: ReportRepository

    @Autowired
    private lateinit var reportJpa: ReportJpaRepository

    private val reviewTarget = ReportTarget(ReportTarget.Type.REVIEW, "00000000-0000-0000-0000-000000000001")
    private val otherReviewTarget = ReportTarget(ReportTarget.Type.REVIEW, "00000000-0000-0000-0000-000000000002")
    private val alice = ReporterId.of("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
    private val bob = ReporterId.of("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")

    private fun report(
        target: ReportTarget = reviewTarget,
        reporterId: ReporterId = alice,
        reason: ReportReason = ReportReason.SPAM,
        description: String? = null,
    ) = Report.create(
        target = target,
        reporterId = reporterId,
        reason = reason,
        description = description,
        createdAt = Instant.parse("2025-11-03T10:24:00Z"),
    )

    @Test
    fun `保存した通報を同じ内容で復元できる`() {
        val original = report(reason = ReportReason.OTHER, description = "営業時間が違う")

        assertTrue(repository.saveIfNotReported(original))

        assertEquals(original, reportJpa.findById(original.id.value).orElseThrow().toDomain())
    }

    @Test
    fun `同じ人が同じ対象を 2 回通報しても 2 件目は保存しない`() {
        assertTrue(repository.saveIfNotReported(report()))

        assertFalse(repository.saveIfNotReported(report(reason = ReportReason.HATE)))
        assertEquals(1, reportJpa.count())
    }

    @Test
    fun `別の人や別の対象なら通報できる`() {
        assertTrue(repository.saveIfNotReported(report()))
        assertTrue(repository.saveIfNotReported(report(reporterId = bob)))
        assertTrue(repository.saveIfNotReported(report(target = otherReviewTarget)))

        assertEquals(3, reportJpa.count())
    }

    @Test
    fun `通報者の通報だけを全件削除する`() {
        repository.saveIfNotReported(report())
        repository.saveIfNotReported(report(target = otherReviewTarget))
        repository.saveIfNotReported(report(reporterId = bob))

        assertEquals(2, repository.deleteByReporterId(alice))

        assertEquals(listOf(bob), reportJpa.findAll().map { it.toDomain().reporterId })
    }

    @Test
    fun `通報が無い通報者を削除しても 0 件で成功する`() {
        assertEquals(0, repository.deleteByReporterId(alice))
    }

    @Test
    fun `未対応の通報だけを対象ごとに引き、対応状況の変更を保存できる`() {
        val first = report()
        val second = report(reporterId = bob)
        repository.saveIfNotReported(first)
        repository.saveIfNotReported(second)
        repository.saveIfNotReported(report(target = otherReviewTarget))
        val operator = ReportHandlerId.of("33333333-3333-3333-3333-333333333333")
        val handledAt = Instant.parse("2026-10-07T03:00:00Z")

        repository.saveAll(listOf(first.handle(ReportStatus.RESOLVED, operator, handledAt)))

        assertEquals(listOf(second.id), repository.findOpenByTarget(reviewTarget).map { it.id })
        val restored = reportJpa.findById(first.id.value).orElseThrow().toDomain()
        assertEquals(ReportStatus.RESOLVED, restored.status)
        assertEquals(operator, restored.handledBy)
        assertEquals(handledAt, restored.handledAt)
    }

    @Test
    fun `対象への通報の有無を対応状況にかかわらず判定する`() {
        assertFalse(repository.existsByTarget(reviewTarget))

        repository.saveIfNotReported(report())

        assertTrue(repository.existsByTarget(reviewTarget))
        assertFalse(repository.existsByTarget(otherReviewTarget))
    }
}

package com.kobeinyourpocket.backend.domain.report

import com.kobeinyourpocket.backend.domain.report.model.Report
import com.kobeinyourpocket.backend.domain.report.vo.ReportReason
import com.kobeinyourpocket.backend.domain.report.vo.ReportStatus
import com.kobeinyourpocket.backend.domain.report.vo.ReportTarget
import com.kobeinyourpocket.backend.domain.report.vo.ReporterId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ReportDomainTest {
    private val target = ReportTarget(type = ReportTarget.Type.REVIEW, id = "00000000-0000-0000-0000-000000000001")
    private val reporter = ReporterId.of("11111111-1111-1111-1111-111111111111")

    private fun create(
        reason: ReportReason,
        description: String?,
    ) = Report.create(target = target, reporterId = reporter, reason = reason, description = description)

    @Test
    fun `新規の通報は受付状態から始まる`() {
        val report = create(ReportReason.SPAM, null)

        assertEquals(ReportStatus.OPEN, report.status)
        assertNull(report.description)
    }

    @Test
    fun `その他以外の理由は詳細なしで通報できる`() {
        ReportReason.entries.filter { it != ReportReason.OTHER }.forEach { reason ->
            create(reason, null)
        }
    }

    @Test
    fun `その他の理由は詳細が必須`() {
        assertFailsWith<IllegalArgumentException> { create(ReportReason.OTHER, null) }
    }

    @Test
    fun `空白だけの詳細は未入力として扱い、その他では受け付けない`() {
        assertNull(create(ReportReason.SPAM, "   ").description)
        assertFailsWith<IllegalArgumentException> { create(ReportReason.OTHER, "   ") }
    }

    @Test
    fun `詳細の前後の空白を落とす`() {
        assertEquals("口コミの内容が事実と違う", create(ReportReason.OTHER, "  口コミの内容が事実と違う \n").description)
    }

    @Test
    fun `詳細は 500 文字まで`() {
        create(ReportReason.OTHER, "あ".repeat(Report.MAX_DESCRIPTION_LENGTH))

        assertFailsWith<IllegalArgumentException> {
            create(ReportReason.OTHER, "あ".repeat(Report.MAX_DESCRIPTION_LENGTH + 1))
        }
    }

    @Test
    fun `理由の code から復元でき、未知の code は null`() {
        assertEquals(ReportReason.SEXUAL_OR_VIOLENT, ReportReason.of("SEXUAL_OR_VIOLENT"))
        assertNull(ReportReason.of("spam"))
        assertNull(ReportReason.of("UNKNOWN"))
    }

    @Test
    fun `通報対象の ID は空にできない`() {
        assertFailsWith<IllegalArgumentException> { ReportTarget(type = ReportTarget.Type.REVIEW, id = " ") }
    }
}

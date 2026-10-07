package com.kobeinyourpocket.backend.infrastructure.rest.report

import com.kobeinyourpocket.backend.domain.report.model.Report
import java.time.Instant

/**
 * 受け付けた通報のレスポンス（#147）。
 *
 * 通報者の id は返さない（送ったのは本人なので不要）。
 */
data class ReportResponse(
    val id: String,
    val targetType: String,
    val targetId: String,
    val reason: String,
    val description: String?,
    val status: String,
    val createdAt: Instant,
) {
    companion object {
        fun from(report: Report): ReportResponse =
            ReportResponse(
                id = report.id.toString(),
                targetType = report.target.type.name,
                targetId = report.target.id,
                reason = report.reason.name,
                description = report.description,
                status = report.status.name,
                createdAt = report.createdAt,
            )
    }
}

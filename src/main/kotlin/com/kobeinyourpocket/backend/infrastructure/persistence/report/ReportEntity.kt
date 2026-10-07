package com.kobeinyourpocket.backend.infrastructure.persistence.report

import com.kobeinyourpocket.backend.domain.report.model.Report
import com.kobeinyourpocket.backend.domain.report.vo.ReportId
import com.kobeinyourpocket.backend.domain.report.vo.ReportReason
import com.kobeinyourpocket.backend.domain.report.vo.ReportStatus
import com.kobeinyourpocket.backend.domain.report.vo.ReportTarget
import com.kobeinyourpocket.backend.domain.report.vo.ReporterId
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant
import java.util.UUID

/**
 * DB `reports`（V20）。
 *
 * 一意制約は Flyway 側が正だが、テスト（H2 / create-drop）でも重複を DB で弾けるよう
 * エンティティにも同じ制約を宣言している。
 */
@Entity
@Table(
    name = "reports",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uq_reports_target_reporter",
            columnNames = ["target_type", "target_id", "reporter_user_id"],
        ),
    ],
)
class ReportEntity(
    @Id
    @Column(name = "id", columnDefinition = "uuid", updatable = false, nullable = false)
    var id: UUID,
    @Column(name = "target_type", nullable = false, length = 16)
    var targetType: String,
    @Column(name = "target_id", nullable = false, length = 128)
    var targetId: String,
    @Column(name = "reporter_user_id", columnDefinition = "uuid", nullable = false)
    var reporterUserId: UUID,
    @Column(name = "reason", nullable = false, length = 32)
    var reason: String,
    @Column(name = "description")
    var description: String? = null,
    @Column(name = "status", nullable = false, length = 16)
    var status: String,
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant,
) {
    fun toDomain(): Report =
        Report(
            id = ReportId.of(id),
            target = ReportTarget(type = ReportTarget.Type.valueOf(targetType), id = targetId),
            reporterId = ReporterId.of(reporterUserId),
            reason = ReportReason.valueOf(reason),
            description = description,
            status = ReportStatus.valueOf(status),
            createdAt = createdAt,
        )

    companion object {
        fun fromDomain(report: Report): ReportEntity =
            ReportEntity(
                id = report.id.value,
                targetType = report.target.type.name,
                targetId = report.target.id,
                reporterUserId = report.reporterId.value,
                reason = report.reason.name,
                description = report.description,
                status = report.status.name,
                createdAt = report.createdAt,
            )
    }
}

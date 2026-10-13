package com.kobeinyourpocket.backend.infrastructure.persistence.report

import com.kobeinyourpocket.backend.domain.report.model.Report
import com.kobeinyourpocket.backend.domain.report.repository.ReportRepository
import com.kobeinyourpocket.backend.domain.report.vo.ReportStatus
import com.kobeinyourpocket.backend.domain.report.vo.ReportTarget
import com.kobeinyourpocket.backend.domain.report.vo.ReporterId
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

/** [ReportRepository] port の outbound adapter。 */
@Repository
class ReportRepositoryImpl(
    private val reportJpa: ReportJpaRepository,
) : ReportRepository {
    /**
     * 通常の重複は事前の存在確認で弾き、同時送信ですり抜けた分は一意制約違反で弾く。
     *
     * このメソッド自体はトランザクションを張らない。張ると一意制約違反の時点で
     * トランザクションが rollback-only になり、捕まえて false を返してもコミット時に
     * 例外になるため。`saveAndFlush` は Spring Data 側の短いトランザクションで完結する。
     */
    override fun saveIfNotReported(report: Report): Boolean {
        val alreadyReported =
            reportJpa.existsByTargetTypeAndTargetIdAndReporterUserId(
                targetType = report.target.type.name,
                targetId = report.target.id,
                reporterUserId = report.reporterId.value,
            )
        if (alreadyReported) return false
        return try {
            reportJpa.saveAndFlush(ReportEntity.fromDomain(report))
            true
        } catch (e: DataIntegrityViolationException) {
            false
        }
    }

    override fun existsByTarget(target: ReportTarget): Boolean =
        reportJpa.existsByTargetTypeAndTargetId(targetType = target.type.name, targetId = target.id)

    override fun findOpenByTarget(target: ReportTarget): List<Report> =
        reportJpa
            .findByTargetTypeAndTargetIdAndStatus(
                targetType = target.type.name,
                targetId = target.id,
                status = ReportStatus.OPEN.name,
            ).map { it.toDomain() }

    @Transactional
    override fun saveAll(reports: List<Report>) {
        reportJpa.saveAll(reports.map(ReportEntity::fromDomain))
    }

    @Transactional
    override fun deleteByReporterId(reporterId: ReporterId): Int = reportJpa.deleteByReporterUserId(reporterId.value)
}

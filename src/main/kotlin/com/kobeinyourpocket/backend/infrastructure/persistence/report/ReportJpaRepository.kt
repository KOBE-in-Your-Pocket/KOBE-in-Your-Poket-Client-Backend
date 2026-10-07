package com.kobeinyourpocket.backend.infrastructure.persistence.report

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ReportJpaRepository : JpaRepository<ReportEntity, UUID> {
    fun existsByTargetTypeAndTargetIdAndReporterUserId(
        targetType: String,
        targetId: String,
        reporterUserId: UUID,
    ): Boolean

    /**
     * 通報者の user id で通報を一括削除する（退会処理）。
     *
     * 1 ユーザー分の通報数は少ないため derived delete query で足りる。
     *
     * @return 削除した件数
     */
    fun deleteByReporterUserId(reporterUserId: UUID): Int
}

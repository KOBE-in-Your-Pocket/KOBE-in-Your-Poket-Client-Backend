package com.kobeinyourpocket.backend.domain.report.model

import com.kobeinyourpocket.backend.domain.report.vo.ReportId
import com.kobeinyourpocket.backend.domain.report.vo.ReportReason
import com.kobeinyourpocket.backend.domain.report.vo.ReportStatus
import com.kobeinyourpocket.backend.domain.report.vo.ReportTarget
import com.kobeinyourpocket.backend.domain.report.vo.ReporterId
import java.time.Instant

/**
 * [エンティティ] 通報。利用者が不適切なコンテンツを運営へ知らせた記録（#147）。
 *
 * 不変条件:
 * - [reason] が [ReportReason.OTHER] のときは [description] 必須（何が問題か選択肢から読み取れないため）
 * - [description] は空白のみを許さず、最大 [MAX_DESCRIPTION_LENGTH] 文字
 *
 * 同じ通報者が同じ対象を 2 回通報できない制約は、集約をまたぐため repository / DB の一意制約で守る。
 */
data class Report(
    val id: ReportId,
    val target: ReportTarget,
    val reporterId: ReporterId,
    val reason: ReportReason,
    val description: String?,
    val status: ReportStatus,
    val createdAt: Instant,
) {
    init {
        if (description != null) {
            require(description.isNotBlank()) { "description must not be blank when given" }
            require(description.length <= MAX_DESCRIPTION_LENGTH) {
                "description must be at most $MAX_DESCRIPTION_LENGTH characters, got ${description.length}"
            }
        }
        require(!reason.requiresDescription || description != null) {
            "description is required when reason is $reason"
        }
    }

    companion object {
        const val MAX_DESCRIPTION_LENGTH = 500

        /**
         * 通報を新規に受け付ける。[ReportId] はサーバー側で採番し、状態は [ReportStatus.OPEN] から始まる。
         *
         * [description] は前後の空白を落とし、空になったら未入力（null）として扱う。
         */
        fun create(
            target: ReportTarget,
            reporterId: ReporterId,
            reason: ReportReason,
            description: String?,
            createdAt: Instant = Instant.now(),
        ): Report =
            Report(
                id = ReportId.generate(),
                target = target,
                reporterId = reporterId,
                reason = reason,
                description = description?.trim()?.ifEmpty { null },
                status = ReportStatus.OPEN,
                createdAt = createdAt,
            )
    }
}

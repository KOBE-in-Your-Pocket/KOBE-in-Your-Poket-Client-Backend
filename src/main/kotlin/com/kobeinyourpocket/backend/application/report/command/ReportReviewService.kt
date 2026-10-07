package com.kobeinyourpocket.backend.application.report.command

import com.kobeinyourpocket.backend.application.report.AlreadyReportedException
import com.kobeinyourpocket.backend.application.report.CannotReportOwnContentException
import com.kobeinyourpocket.backend.application.tourism.ReviewNotFoundException
import com.kobeinyourpocket.backend.domain.report.model.Report
import com.kobeinyourpocket.backend.domain.report.repository.ReportRepository
import com.kobeinyourpocket.backend.domain.report.vo.ReportReason
import com.kobeinyourpocket.backend.domain.report.vo.ReportTarget
import com.kobeinyourpocket.backend.domain.report.vo.ReporterId
import com.kobeinyourpocket.backend.domain.tourism.review.repository.ReviewRepository
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewAuthorId
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewId
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId
import org.springframework.stereotype.Service

/**
 * レビュー通報ユースケース（#147）。App Store Guideline 1.2 のレビュー解禁条件。
 *
 * 受け付けるのは DB 保存まで。通報されたレビューを自動で非表示にはせず、運営が確認して
 * 必要ならモデレーション削除（`DELETE /api/v1/tourism/reviews/{reviewId}`）する。
 *
 * 判定順:
 * 1. レビューが存在し、パスの [SpotId] 配下にあること（違えば 404）
 * 2. 通報者が投稿者本人でないこと（本人なら 400）
 * 3. 同じ通報者が同じレビューを未通報であること（通報済みなら 409）
 *
 * [reporterId] は REST 層が JWT の `sub` から渡す（自己申告ではない）。
 */
@Service
class ReportReviewService(
    private val reviewRepository: ReviewRepository,
    private val reportRepository: ReportRepository,
) {
    fun execute(
        spotId: SpotId,
        reviewId: ReviewId,
        reporterId: ReporterId,
        reason: ReportReason,
        description: String?,
    ): Report {
        val review = reviewRepository.findById(reviewId) ?: throw ReviewNotFoundException(reviewId)
        if (review.spotId != spotId) throw ReviewNotFoundException(reviewId)

        val target = ReportTarget(type = ReportTarget.Type.REVIEW, id = reviewId.value.toString())
        if (review.author.isOwnedBy(ReviewAuthorId.of(reporterId.value))) {
            throw CannotReportOwnContentException(target)
        }

        val report =
            Report.create(
                target = target,
                reporterId = reporterId,
                reason = reason,
                description = description,
            )
        if (!reportRepository.saveIfNotReported(report)) throw AlreadyReportedException(target)
        return report
    }
}

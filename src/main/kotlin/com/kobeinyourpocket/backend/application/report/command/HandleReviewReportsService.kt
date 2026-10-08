package com.kobeinyourpocket.backend.application.report.command

import com.kobeinyourpocket.backend.application.report.ReportsNotFoundException
import com.kobeinyourpocket.backend.domain.report.repository.ReportRepository
import com.kobeinyourpocket.backend.domain.report.vo.ReportHandlerId
import com.kobeinyourpocket.backend.domain.report.vo.ReportStatus
import com.kobeinyourpocket.backend.domain.report.vo.ReportTarget
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * 口コミへの通報に運営が対応するユースケース（#145）。
 *
 * 運営向け一覧は口コミ単位でまとめて表示するため、更新も口コミ単位で行う。その口コミへの
 * **未対応（OPEN）の通報をまとめて** [ReportStatus.APPROVED] か [ReportStatus.DISMISSED] にする。
 * すでに対応済みの通報は触らない（担当者・日時の履歴を上書きしない）。
 *
 * 口コミ自体の存在は問わない。削除済みの口コミへの通報も、一覧に残っていれば閉じられる。
 */
@Service
class HandleReviewReportsService(
    private val reportRepository: ReportRepository,
) {
    /**
     * @return 対応状況を変えた通報の件数。全件対応済みなら 0（冪等）
     * @throws ReportsNotFoundException その口コミへの通報が 1 件も無い
     */
    @Transactional
    fun execute(
        reviewId: ReviewId,
        status: ReportStatus,
        handledBy: ReportHandlerId,
        handledAt: Instant = Instant.now(),
    ): Int {
        val target = ReportTarget(type = ReportTarget.Type.REVIEW, id = reviewId.value.toString())
        if (!reportRepository.existsByTarget(target)) throw ReportsNotFoundException(target)
        return handleOpenReports(target, status, handledBy, handledAt)
    }

    /**
     * 運営がモデレーション削除した口コミへの未対応の通報を、承認（[ReportStatus.APPROVED]）にする（#145）。
     *
     * 削除したのに一覧へ「未対応」として残り続けるのを防ぐ。通報が無い口コミの削除でも
     * 呼ばれるため、0 件でも例外にしない。
     */
    @Transactional
    fun approveOnReviewDeleted(
        reviewId: ReviewId,
        handledBy: ReportHandlerId,
        handledAt: Instant = Instant.now(),
    ): Int {
        val target = ReportTarget(type = ReportTarget.Type.REVIEW, id = reviewId.value.toString())
        return handleOpenReports(target, ReportStatus.APPROVED, handledBy, handledAt)
    }

    private fun handleOpenReports(
        target: ReportTarget,
        status: ReportStatus,
        handledBy: ReportHandlerId,
        handledAt: Instant,
    ): Int {
        val handled = reportRepository.findOpenByTarget(target).map { it.handle(status, handledBy, handledAt) }
        if (handled.isNotEmpty()) reportRepository.saveAll(handled)
        return handled.size
    }
}

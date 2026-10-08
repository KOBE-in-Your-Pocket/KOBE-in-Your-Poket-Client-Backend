package com.kobeinyourpocket.backend.application.report.query

import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.domain.report.vo.ReportStatus
import org.springframework.stereotype.Service

/**
 * 運営向け通報一覧ユースケース（read / #145）。domain 集約を経由せず [ReviewReportQuery] port へ委譲する。
 *
 * ページ境界の正規化は他の運営向け一覧（ListAllReviewsService / ListUsersService）と同じ方針。
 */
@Service
class ListReviewReportsService(
    private val reviewReportQuery: ReviewReportQuery,
) {
    /**
     * [page]（0 始まり）/ [size] は未指定可。負値は下限へ、上限超過は [MAX_SIZE] へ丸める。
     * [status] が null なら対応状況で絞らない。
     */
    fun listReviewReports(
        language: Language,
        status: ReportStatus? = null,
        page: Int? = null,
        size: Int? = null,
    ): ReviewReportPageView =
        reviewReportQuery.findPage(
            status = status,
            page = (page ?: 0).coerceAtLeast(0),
            size = (size ?: DEFAULT_SIZE).coerceIn(1, MAX_SIZE),
            language = language,
        )

    companion object {
        /** 管理画面の 1 画面分として十分な件数。1 件に通報の明細を含むため口コミ一覧より少なめ。 */
        const val DEFAULT_SIZE = 20

        /** 1 リクエストで返す上限。 */
        const val MAX_SIZE = 100
    }
}

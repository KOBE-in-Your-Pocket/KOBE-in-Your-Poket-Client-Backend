package com.kobeinyourpocket.backend.application.report.query

import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.domain.report.vo.ReportStatus

/** read 専用 port（#145）。application が定義し infrastructure.query が実装する。 */
interface ReviewReportQuery {
    /**
     * 通報された口コミを、未対応の通報が多い順（同数なら最新の通報が新しい順）に返す。
     *
     * [status] を指定すると、その状態の通報が 1 件以上ある口コミだけに絞る。
     * [language] はスポット名の解決にだけ効く。
     */
    fun findPage(
        status: ReportStatus?,
        page: Int,
        size: Int,
        language: Language,
    ): ReviewReportPageView
}

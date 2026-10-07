package com.kobeinyourpocket.backend.application.report

import com.kobeinyourpocket.backend.domain.report.vo.ReportTarget

/**
 * 自分の投稿を通報しようとした場合の例外（REST では 400 / #147）。
 *
 * Client は自分の投稿のメニューに「通報」を出さない（編集・削除を出す）。
 * それでも API を直接叩かれたときに、運営の確認対象へ無意味な通報が混ざらないよう弾く。
 */
class CannotReportOwnContentException(
    target: ReportTarget,
) : RuntimeException("Cannot report own content: ${target.type} ${target.id}")

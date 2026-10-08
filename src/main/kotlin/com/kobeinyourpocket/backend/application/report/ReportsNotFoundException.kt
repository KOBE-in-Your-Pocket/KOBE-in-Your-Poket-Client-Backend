package com.kobeinyourpocket.backend.application.report

import com.kobeinyourpocket.backend.domain.report.vo.ReportTarget

/**
 * 対象への通報が 1 件も無い場合の例外（REST では 404 / #145）。
 *
 * 通報の無い口コミの対応状況を変えようとした（ID の取り違えなど）ことを運営に知らせる。
 * 通報はあるが全件対応済みの場合はこれにならず、0 件更新として成功する。
 */
class ReportsNotFoundException(
    target: ReportTarget,
) : RuntimeException("No reports found for ${target.type} ${target.id}")

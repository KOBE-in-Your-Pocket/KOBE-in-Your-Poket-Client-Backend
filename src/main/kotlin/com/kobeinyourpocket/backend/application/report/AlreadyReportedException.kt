package com.kobeinyourpocket.backend.application.report

import com.kobeinyourpocket.backend.domain.report.vo.ReportTarget

/**
 * 同じ通報者が同じ対象をすでに通報している場合の例外（REST では 409 / #147）。
 *
 * 409 にするのは、リクエスト自体は正しくサーバー側の状態（通報済み）が受付を許さないため。
 * Client はこれを見て「すでに通報済みです」と表示できる。
 */
class AlreadyReportedException(
    target: ReportTarget,
) : RuntimeException("Already reported: ${target.type} ${target.id}")

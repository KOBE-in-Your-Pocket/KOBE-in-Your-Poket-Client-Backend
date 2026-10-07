package com.kobeinyourpocket.backend.domain.report

/**
 * Report（通報） — bounded context マーカー（#147 / #145）。
 *
 * 配置: domain/report · application/report · infrastructure/{persistence,rest}/report
 * 利用者が不適切なコンテンツを運営へ知らせる窓口。App Store Guideline 1.2（UGC）の
 * レビュー投稿解禁条件。通報対象のコンテキスト（tourism 等）には ID で参照するだけで、
 * 対象の存在確認は application 層が各コンテキストの port を使って行う。
 *
 * @see docs/architecture.md §3
 */
internal object ReportContext

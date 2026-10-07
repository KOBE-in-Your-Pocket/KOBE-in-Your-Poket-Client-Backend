package com.kobeinyourpocket.backend.domain.report.vo

/**
 * [値オブジェクト] 通報の対応状況。
 *
 * 現状は受付（[OPEN]）のみ。運営が対応済み・却下にする操作は #145 の運営向け一覧と合わせて追加する。
 * 追加時は DB の CHECK 制約（`ck_reports_status`）も新しいマイグレーションで広げる。
 */
enum class ReportStatus {
    OPEN,
}

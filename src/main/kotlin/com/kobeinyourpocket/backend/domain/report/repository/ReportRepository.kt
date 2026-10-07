package com.kobeinyourpocket.backend.domain.report.repository

import com.kobeinyourpocket.backend.domain.report.model.Report
import com.kobeinyourpocket.backend.domain.report.vo.ReporterId

/** [リポジトリ] write port（command）。運営向けの一覧（read）は #145 で query 側に置く。 */
interface ReportRepository {
    /**
     * 通報を保存する。同じ通報者が同じ対象をすでに通報していれば保存せず false を返す。
     *
     * 事前の存在確認だけでは同時送信をすり抜けるため、実装は DB の一意制約違反も
     * false に読み替える。
     *
     * @return 保存したら true、重複で保存しなかったら false
     */
    fun saveIfNotReported(report: Report): Boolean

    /**
     * 指定した通報者の通報を全件削除する。退会処理から呼ぶ。該当 0 件でも例外にしない。
     *
     * @return 削除した件数
     */
    fun deleteByReporterId(reporterId: ReporterId): Int
}

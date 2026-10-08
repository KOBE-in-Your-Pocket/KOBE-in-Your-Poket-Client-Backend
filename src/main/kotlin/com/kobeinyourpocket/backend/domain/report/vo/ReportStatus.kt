package com.kobeinyourpocket.backend.domain.report.vo

/**
 * [値オブジェクト] 通報の対応状況（#145）。
 *
 * [OPEN] から [APPROVED] か [REJECTED] へ一度だけ進む。対応済みを未対応へ戻す操作は持たない
 * （誤って閉じた場合でも、同じ対象への新しい通報は別の人から届く）。
 * 値を増やすときは DB の CHECK 制約（`ck_reports_status`）も新しいマイグレーションで広げる。
 */
enum class ReportStatus {
    /** 未対応。運営の確認待ち。 */
    OPEN,

    /** 承認。通報の内容を認め、運営が口コミを削除した。管理画面の「承認済み」。 */
    APPROVED,

    /** 拒否。確認した結果、問題なしと判断した（口コミは残る）。管理画面の「拒否済み」。 */
    REJECTED,
    ;

    companion object {
        /** code から復元する。未知の code は null（呼び出し側で 400 にする）。 */
        fun of(code: String): ReportStatus? = entries.firstOrNull { it.name == code }
    }
}

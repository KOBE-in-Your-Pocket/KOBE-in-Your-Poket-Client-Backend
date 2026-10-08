package com.kobeinyourpocket.backend.domain.report.vo

/**
 * [値オブジェクト] 通報の対応状況（#145）。
 *
 * [OPEN] から [APPROVED] か [DISMISSED] へ一度だけ進み、どちらも「対応済み」として扱う。対応済みを未対応へ戻す操作は持たない
 * （誤って閉じた場合でも、同じ対象への新しい通報は別の人から届く）。
 * 値を増やすときは DB の CHECK 制約（`ck_reports_status`）も新しいマイグレーションで広げる。
 */
enum class ReportStatus {
    /** 未対応。運営の確認待ち。 */
    OPEN,

    /**
     * 承認。運営が通報を認め、口コミを利用規約違反としてアプリ全体での非表示に同意した。
     * 運営による口コミの削除もこれになる。
     */
    APPROVED,

    /** 却下。確認した結果、非表示にするほどではない（問題なし）と判断した。 */
    DISMISSED,
    ;

    companion object {
        /** code から復元する。未知の code は null（呼び出し側で 400 にする）。 */
        fun of(code: String): ReportStatus? = entries.firstOrNull { it.name == code }
    }
}

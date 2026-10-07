package com.kobeinyourpocket.backend.domain.report.vo

/**
 * [値オブジェクト] 通報理由。Client のラジオボタンの選択肢に 1 対 1 で対応する。
 *
 * 表示文言（4 言語）は Client が持ち、API では [name] の code だけをやり取りする。
 * 選択肢を増やすときは DB の CHECK 制約（`ck_reports_reason`）も新しいマイグレーションで広げる。
 */
enum class ReportReason {
    /** スパム・宣伝目的 */
    SPAM,

    /** 誹謗中傷・嫌がらせ */
    HARASSMENT,

    /** 差別的・ヘイト表現 */
    HATE,

    /** 性的・暴力的な内容 */
    SEXUAL_OR_VIOLENT,

    /** 個人情報の掲載 */
    PERSONAL_INFO,

    /** 虚偽・スポットと無関係な内容 */
    MISLEADING,

    /** その他。選択肢に当てはまらないため、自由記述を必須にする */
    OTHER,
    ;

    /** この理由で自由記述（詳細）が必須か。 */
    val requiresDescription: Boolean
        get() = this == OTHER

    companion object {
        /** code から復元する。未知の code は null（呼び出し側で 400 にする）。 */
        fun of(code: String): ReportReason? = entries.firstOrNull { it.name == code }
    }
}

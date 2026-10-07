package com.kobeinyourpocket.backend.domain.report.vo

/**
 * [値オブジェクト] 通報対象。種別と、その種別内での ID の組。
 *
 * [id] は対象コンテキストの ID を文字列で持つ（レビューは UUID、スポットは slug と形式が違うため）。
 * 対象への外部キーは張らない。対象が削除されても、運営の対応履歴として通報は残す（#147）。
 */
data class ReportTarget(
    val type: Type,
    val id: String,
) {
    init {
        require(id.isNotBlank()) { "report target id must not be blank" }
        require(id.length <= MAX_ID_LENGTH) {
            "report target id must be at most $MAX_ID_LENGTH characters, got ${id.length}"
        }
    }

    /**
     * 通報できる対象の種別。
     *
     * 現状はレビューのみ（#147）。スポット・避難所などは #145 で追加する。
     * 追加時は DB の CHECK 制約（`ck_reports_target_type`）も新しいマイグレーションで広げる。
     */
    enum class Type {
        REVIEW,
    }

    companion object {
        const val MAX_ID_LENGTH = 128
    }
}

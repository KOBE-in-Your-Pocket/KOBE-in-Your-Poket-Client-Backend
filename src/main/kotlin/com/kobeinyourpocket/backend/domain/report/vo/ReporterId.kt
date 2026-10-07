package com.kobeinyourpocket.backend.domain.report.vo

import java.util.UUID

/**
 * [値オブジェクト] 通報者の識別子（Supabase Auth の user id）。
 *
 * 実体は `domain.user` の `User.Id` と同じ UUID だが、report から user コンテキストの
 * モデルを直接参照しないよう、report 側の VO として持つ。
 * 重複通報の判定（同じ人が同じ対象を 2 回通報しない）と、退会時の削除に使う。
 */
@JvmInline
value class ReporterId private constructor(
    val value: UUID,
) {
    override fun toString(): String = value.toString()

    companion object {
        fun of(value: UUID): ReporterId = ReporterId(value)

        /**
         * UUID 文字列から復元する。不正な形式は [IllegalArgumentException] をスローする。
         */
        fun of(value: String): ReporterId =
            try {
                ReporterId(UUID.fromString(value))
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Invalid ReporterId format: $value", e)
            }
    }
}

package com.kobeinyourpocket.backend.domain.report.vo

import java.util.UUID

/**
 * [値オブジェクト] 通報に対応した運営の識別子（Supabase Auth の user id / #145）。
 *
 * 「誰がいつ閉じたか」を後から辿るために残す。通報者（[ReporterId]）とは役割が違うため型を分ける。
 */
@JvmInline
value class ReportHandlerId private constructor(
    val value: UUID,
) {
    override fun toString(): String = value.toString()

    companion object {
        fun of(value: UUID): ReportHandlerId = ReportHandlerId(value)

        /**
         * UUID 文字列から復元する。不正な形式は [IllegalArgumentException] をスローする。
         */
        fun of(value: String): ReportHandlerId =
            try {
                ReportHandlerId(UUID.fromString(value))
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Invalid ReportHandlerId format: $value", e)
            }
    }
}

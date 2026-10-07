package com.kobeinyourpocket.backend.domain.report.vo

import java.util.UUID

/**
 * [値オブジェクト] 通報識別子（UUID）。
 *
 * [generate] がサーバー側採番の入口。既存 UUID からの復元は [of] を使う。
 */
@JvmInline
value class ReportId private constructor(
    val value: UUID,
) {
    override fun toString(): String = value.toString()

    companion object {
        fun generate(): ReportId = ReportId(UUID.randomUUID())

        fun of(value: UUID): ReportId = ReportId(value)
    }
}

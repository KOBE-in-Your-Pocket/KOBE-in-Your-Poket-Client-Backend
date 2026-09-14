package com.kobeinyourpocket.backend.infrastructure.query.common

import java.nio.ByteBuffer
import java.util.UUID

/**
 * native query が返す UUID カラムを文字列へ正規化する（CQRS read 側の共通処理）。
 *
 * [JdbcTimestamps] と同じ問題への対処。`createNativeQuery` の結果は `Array<Any?>` で受けるため
 * UUID 列の Java 型もドライバ依存で、PostgreSQL（pgjdbc）は [UUID]、H2 は 16 バイトの
 * [ByteArray] を返す。素朴に `toString()` すると H2 側だけ `[B@1a2b3c` のような
 * 参照文字列になり、**テスト（H2）では気づけず本番だけ正しい**という逆向きの取りこぼしになる。
 *
 * 対応する型を増やすときは JdbcUuidsTest に必ずケースを足すこと。
 */
object JdbcUuids {
    private const val UUID_BYTES = 16

    /** UUID 列を文字列に変換する。NULL 許容列は [toUuidStringOrNull] を使う。 */
    fun toUuidString(value: Any?): String = toUuidStringOrNull(value) ?: error("Unexpected null in a non-null UUID column")

    /** NULL 許容の UUID 列を文字列に変換する。 */
    fun toUuidStringOrNull(value: Any?): String? =
        when (value) {
            null -> null
            is UUID -> value.toString()
            is String -> UUID.fromString(value).toString()
            is ByteArray -> fromBytes(value).toString()
            else -> error("Unsupported uuid type: ${value::class}")
        }

    private fun fromBytes(bytes: ByteArray): UUID {
        require(bytes.size == UUID_BYTES) { "UUID must be $UUID_BYTES bytes, got ${bytes.size}" }
        val buffer = ByteBuffer.wrap(bytes)
        return UUID(buffer.long, buffer.long)
    }
}

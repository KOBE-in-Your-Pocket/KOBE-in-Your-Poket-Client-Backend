package com.kobeinyourpocket.backend.infrastructure.query.common

import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime

/**
 * native query が返す日時カラムを java.time へ正規化する（CQRS read 側の共通処理）。
 *
 * `createNativeQuery` の結果は `Array<Any?>` で受けるため、各カラムの Java 型は JDBC ドライバ依存になる。
 * TIMESTAMPTZ は PostgreSQL（pgjdbc）が [Instant]、H2 が [Timestamp] を返すというように実行環境で
 * 変わるので、QueryJpa ごとに分岐を書くと片方の型を取りこぼす。実際 [Instant] の分岐漏れで
 * 避難所一覧 API が本番（PostgreSQL）だけ 500 になり、テスト（H2）では再現しなかった。変換はここに集約する。
 *
 * テストは H2 で動くため、未対応の型が来ても統合テストでは再現しない。
 * 対応する型を増やすときは JdbcTimestampsTest に必ずケースを足すこと。
 */
object JdbcTimestamps {
    /** TIMESTAMPTZ 列を [Instant] に変換する。 */
    fun toInstant(value: Any?): Instant =
        when (value) {
            is Instant -> value
            is OffsetDateTime -> value.toInstant()
            is Timestamp -> value.toInstant()
            else -> error("Unsupported timestamp type: ${value?.let { it::class }}")
        }

    /** DATE 列を [LocalDate] に変換する。 */
    fun toLocalDate(value: Any?): LocalDate =
        when (value) {
            is LocalDate -> value
            is java.sql.Date -> value.toLocalDate()
            else -> error("Unsupported date type: ${value?.let { it::class }}")
        }
}

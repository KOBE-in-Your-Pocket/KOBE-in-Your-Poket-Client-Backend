package com.kobeinyourpocket.backend.infrastructure.query.common

import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * native query の日時カラム変換。
 *
 * ドライバごとに返る型が違うため（PostgreSQL は [Instant]、H2 は [Timestamp]）、
 * 統合テストは H2 の型しか通らない。ここで全ドライバ分の型を明示的に押さえる。
 */
class JdbcTimestampsTest {
    private val instant = Instant.parse("2025-04-02T01:23:45Z")

    @Test
    fun `toInstant は PostgreSQL が返す Instant をそのまま返す`() {
        assertEquals(instant, JdbcTimestamps.toInstant(instant))
    }

    @Test
    fun `toInstant は OffsetDateTime を Instant にする`() {
        assertEquals(instant, JdbcTimestamps.toInstant(instant.atOffset(ZoneOffset.ofHours(9))))
    }

    @Test
    fun `toInstant は H2 が返す Timestamp を Instant にする`() {
        assertEquals(instant, JdbcTimestamps.toInstant(Timestamp.from(instant)))
    }

    @Test
    fun `toInstant は未対応の型を握り潰さず失敗する`() {
        assertFailsWith<IllegalStateException> { JdbcTimestamps.toInstant("2025-04-02T01:23:45Z") }
    }

    @Test
    fun `toInstant は null を握り潰さず失敗する`() {
        assertFailsWith<IllegalStateException> { JdbcTimestamps.toInstant(null) }
    }

    @Test
    fun `toLocalDate は LocalDate をそのまま返す`() {
        assertEquals(LocalDate.of(2025, 4, 2), JdbcTimestamps.toLocalDate(LocalDate.of(2025, 4, 2)))
    }

    @Test
    fun `toLocalDate は java sql Date を LocalDate にする`() {
        assertEquals(LocalDate.of(2025, 4, 2), JdbcTimestamps.toLocalDate(java.sql.Date.valueOf(LocalDate.of(2025, 4, 2))))
    }

    @Test
    fun `toLocalDate は未対応の型を握り潰さず失敗する`() {
        assertFailsWith<IllegalStateException> { JdbcTimestamps.toLocalDate("2025-04-02") }
    }
}

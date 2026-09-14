package com.kobeinyourpocket.backend.infrastructure.query.common

import java.nio.ByteBuffer
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class JdbcUuidsTest {
    private val uuid = UUID.fromString("11111111-2222-3333-4444-555555555555")

    @Test
    fun `pgjdbc が返す UUID をそのまま文字列にする`() {
        assertEquals(uuid.toString(), JdbcUuids.toUuidString(uuid))
    }

    @Test
    fun `H2 が返す 16 バイト配列を UUID 文字列にする`() {
        val bytes =
            ByteBuffer
                .allocate(16)
                .putLong(uuid.mostSignificantBits)
                .putLong(uuid.leastSignificantBits)
                .array()

        assertEquals(uuid.toString(), JdbcUuids.toUuidString(bytes))
    }

    @Test
    fun `文字列で返るドライバも受け付ける`() {
        assertEquals(uuid.toString(), JdbcUuids.toUuidString(uuid.toString()))
    }

    @Test
    fun `NULL 許容列の null は null のまま返す`() {
        assertNull(JdbcUuids.toUuidStringOrNull(null))
    }

    @Test
    fun `非 NULL 列に null が来たら落とす`() {
        assertFailsWith<IllegalStateException> { JdbcUuids.toUuidString(null) }
    }

    @Test
    fun `未対応の型は落とす`() {
        assertFailsWith<IllegalStateException> { JdbcUuids.toUuidString(42) }
    }

    @Test
    fun `16 バイトでないバイト列は落とす`() {
        assertFailsWith<IllegalArgumentException> { JdbcUuids.toUuidString(ByteArray(8)) }
    }
}

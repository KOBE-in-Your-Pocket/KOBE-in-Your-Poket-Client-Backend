package com.kobeinyourpocket.backend.application.media.command

import com.kobeinyourpocket.backend.application.media.MediaStorage
import org.springframework.util.unit.DataSize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class UploadMediaServiceTest {
    private class FakeMediaStorage : MediaStorage {
        var lastKey: String? = null
        var lastContentType: String? = null
        var lastBytes: ByteArray? = null

        override fun store(
            key: String,
            bytes: ByteArray,
            contentType: String,
        ): String {
            lastKey = key
            lastBytes = bytes
            lastContentType = contentType
            return "https://cdn.example.com/$key"
        }

        // アップロード単体では確定・差し戻しは起きない（呼ばれたら分かるよう記録だけする）。
        var committed: MutableList<String> = mutableListOf()
        var released: MutableList<String> = mutableListOf()

        override fun commit(imageUrl: String): Boolean = committed.add(imageUrl)

        override fun release(imageUrl: String): Boolean = released.add(imageUrl)
    }

    private val maxBytes = 5L * 1024 * 1024
    private val storage = FakeMediaStorage()
    private val service = UploadMediaService(storage, DataSize.ofBytes(maxBytes))

    /** magic bytes を含む最小のダミー画像バイト列。 */
    private fun jpeg() = bytesOf(0xFF, 0xD8, 0xFF, 0xE0, 0x00, 0x10)

    private fun png() = bytesOf(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00)

    private fun webp() = bytesOf(0x52, 0x49, 0x46, 0x46, 0x1A, 0x00, 0x00, 0x00, 0x57, 0x45, 0x42, 0x50)

    private fun bytesOf(vararg ints: Int): ByteArray = ints.map { it.toByte() }.toByteArray()

    @Test
    fun `実体が JPEG なら jpg として保存し公開 URL を返す`() {
        val url = service.upload(bytes = jpeg(), contentType = "image/jpeg")

        assertTrue(url.startsWith("https://cdn.example.com/uploads/"))
        assertTrue(url.endsWith(".jpg"))
        assertEquals("image/jpeg", storage.lastContentType)
    }

    @Test
    fun `content-type にパラメータが付いていても実体判定で保存する`() {
        val url = service.upload(png(), "image/png; charset=binary")

        assertTrue(url.endsWith(".png"))
        assertEquals("image/png", storage.lastContentType)
    }

    @Test
    fun `WebP を webp として保存する`() {
        val url = service.upload(webp(), "image/webp")

        assertTrue(url.endsWith(".webp"))
        assertEquals("image/webp", storage.lastContentType)
    }

    @Test
    fun `content-type が無くても実体が画像なら保存する（実形式で判定）`() {
        val url = service.upload(png(), null)

        assertTrue(url.endsWith(".png"))
        assertEquals("image/png", storage.lastContentType)
    }

    @Test
    fun `任意バイト列を image と偽っても magic bytes 不一致で拒否する`() {
        // image/jpeg と申告するが中身は画像ではない
        assertFailsWith<IllegalArgumentException> {
            service.upload(bytesOf(0x25, 0x50, 0x44, 0x46), "image/jpeg") // "%PDF"
        }
    }

    @Test
    fun `申告 MIME が実体と食い違う画像は拒否する`() {
        // 中身は PNG だが image/jpeg と申告
        assertFailsWith<IllegalArgumentException> {
            service.upload(png(), "image/jpeg")
        }
    }

    @Test
    fun `空ファイルは弾く`() {
        assertFailsWith<IllegalArgumentException> {
            service.upload(ByteArray(0), "image/jpeg")
        }
    }

    @Test
    fun `サイズ上限を超えると弾く`() {
        assertFailsWith<IllegalArgumentException> {
            service.upload(ByteArray((maxBytes + 1).toInt()), "image/jpeg")
        }
    }

    @Test
    fun `アップロードしただけでは確定しない（staging のまま清理対象に残す）`() {
        service.upload(jpeg(), "image/jpeg")

        assertTrue(storage.committed.isEmpty())
        assertTrue(storage.released.isEmpty())
    }
}

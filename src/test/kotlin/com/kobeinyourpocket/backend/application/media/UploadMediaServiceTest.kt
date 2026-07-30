package com.kobeinyourpocket.backend.application.media

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
    }

    private val storage = FakeMediaStorage()
    private val service = UploadMediaService(storage)

    @Test
    fun `保存してサーバー採番のキーで公開 URL を返す`() {
        val url = service.upload(bytes = byteArrayOf(1, 2, 3), contentType = "image/jpeg")

        assertTrue(url.startsWith("https://cdn.example.com/uploads/"))
        assertTrue(url.endsWith(".jpg"))
        assertEquals("image/jpeg", storage.lastContentType)
        assertTrue(storage.lastKey!!.startsWith("uploads/"))
    }

    @Test
    fun `content-type にパラメータが付いていても正規化して許可する`() {
        val url = service.upload(byteArrayOf(1), "image/png; charset=binary")

        assertTrue(url.endsWith(".png"))
        assertEquals("image/png", storage.lastContentType)
    }

    @Test
    fun `未対応の content-type は弾く`() {
        assertFailsWith<IllegalArgumentException> {
            service.upload(byteArrayOf(1), "application/pdf")
        }
    }

    @Test
    fun `content-type が無い場合は弾く`() {
        assertFailsWith<IllegalArgumentException> {
            service.upload(byteArrayOf(1), null)
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
            service.upload(ByteArray(UploadMediaService.MAX_BYTES + 1), "image/jpeg")
        }
    }
}

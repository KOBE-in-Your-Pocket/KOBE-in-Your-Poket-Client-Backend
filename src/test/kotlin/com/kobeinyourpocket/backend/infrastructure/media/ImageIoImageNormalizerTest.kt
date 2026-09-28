package com.kobeinyourpocket.backend.infrastructure.media

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * アップロード画像の正規化（#184）。
 *
 * 一番大事なのは **EXIF が残らない**こと。アイコンは公開 URL で配るため、写真に埋まった
 * GPS 座標がそのまま出ていくと撮影場所（自宅等）が誰でも読める。
 */
class ImageIoImageNormalizerTest {
    private val normalizer = ImageIoImageNormalizer()

    /** EXIF の識別子。JPEG の APP1 セグメント先頭に入る。 */
    private val exifMarker = byteArrayOf(0x45, 0x78, 0x69, 0x66, 0x00, 0x00)

    private fun jpeg(
        width: Int,
        height: Int,
    ): ByteArray {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()
        graphics.color = Color.BLUE
        graphics.fillRect(0, 0, width, height)
        graphics.dispose()
        val out = ByteArrayOutputStream()
        ImageIO.write(image, "jpg", out)
        return out.toByteArray()
    }

    private fun transparentPng(
        width: Int,
        height: Int,
    ): ByteArray {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val out = ByteArrayOutputStream()
        ImageIO.write(image, "png", out)
        return out.toByteArray()
    }

    /**
     * JPEG の APP1（EXIF）セグメントを SOI の直後に差し込む。
     * 実機の写真を用意せずに「EXIF 付きの入力」を作るための細工。
     */
    private fun withExifSegment(jpeg: ByteArray): ByteArray {
        val payload = exifMarker + ByteArray(32) { 0x20 }
        val length = payload.size + 2
        val segment =
            byteArrayOf(
                0xFF.toByte(),
                0xE1.toByte(),
                (length shr 8).toByte(),
                (length and 0xFF).toByte(),
            ) + payload
        // 先頭 2 バイトは SOI（FFD8）。その直後にセグメントを挟む。
        return jpeg.copyOfRange(0, 2) + segment + jpeg.copyOfRange(2, jpeg.size)
    }

    private fun ByteArray.containsBytes(needle: ByteArray): Boolean =
        (0..(size - needle.size)).any { start ->
            needle.indices.all { this[start + it] == needle[it] }
        }

    private fun sizeOf(bytes: ByteArray): Pair<Int, Int> {
        val image = assertNotNull(ImageIO.read(bytes.inputStream()), "出力を画像として読めない")
        return image.width to image.height
    }

    @Test
    fun `長辺を maxEdge に収め、縦横比を保つ`() {
        val normalized = normalizer.normalize(jpeg(1000, 500), maxEdge = 256)

        val (width, height) = sizeOf(normalized.bytes)
        assertEquals(256, width)
        assertEquals(128, height)
    }

    @Test
    fun `maxEdge より小さい画像は拡大しない`() {
        val normalized = normalizer.normalize(jpeg(64, 48), maxEdge = 256)

        assertEquals(64 to 48, sizeOf(normalized.bytes))
    }

    @Test
    fun `出力は JPEG で、content type と拡張子もそれに合わせる`() {
        val normalized = normalizer.normalize(jpeg(300, 300), maxEdge = 256)

        assertEquals("image/jpeg", normalized.contentType)
        assertEquals("jpg", normalized.extension)
        // SOI マーカー（FFD8）で始まる。
        assertEquals(0xFF.toByte(), normalized.bytes[0])
        assertEquals(0xD8.toByte(), normalized.bytes[1])
    }

    @Test
    fun `EXIF セグメントは出力に残らない`() {
        val input = withExifSegment(jpeg(300, 300))
        assertTrue(input.containsBytes(exifMarker), "前提: 入力には EXIF が入っている")

        val normalized = normalizer.normalize(input, maxEdge = 256)

        assertTrue(!normalized.bytes.containsBytes(exifMarker), "出力に EXIF が残っている")
    }

    @Test
    fun `透過 PNG も JPEG として書き出せる`() {
        // 透過のまま JPEG へ渡すと ImageIO が書き込みに失敗する（または黒く沈む）。
        val normalized = normalizer.normalize(transparentPng(300, 300), maxEdge = 256)

        assertEquals("image/jpeg", normalized.contentType)
        assertEquals(256 to 256, sizeOf(normalized.bytes))
    }

    @Test
    fun `画像でないバイト列は受け付けない`() {
        assertFailsWith<IllegalArgumentException> {
            normalizer.normalize("これは画像ではない".toByteArray(), maxEdge = 256)
        }
    }

    @Test
    fun `空のバイト列は受け付けない`() {
        assertFailsWith<IllegalArgumentException> { normalizer.normalize(ByteArray(0), maxEdge = 256) }
    }
}

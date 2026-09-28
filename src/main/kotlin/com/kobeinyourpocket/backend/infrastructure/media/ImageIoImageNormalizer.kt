package com.kobeinyourpocket.backend.infrastructure.media

import com.kobeinyourpocket.backend.application.media.ImageNormalizer
import net.coobird.thumbnailator.Thumbnails
import org.springframework.stereotype.Component
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

/**
 * [ImageNormalizer] の実装。JPEG へ再エンコードして EXIF を落とす（#184）。
 *
 * 縮小は Thumbnailator に任せる。JDK の ImageIO だけだと **EXIF の Orientation を見ない**ため、
 * 縦向きで撮った写真が横倒しで保存される。Thumbnailator は読み込み時に向きを補正する。
 *
 * 透過 PNG をそのまま JPEG にすると透過部分が黒く沈むため、白地に合成してから書き出す。
 * アニメーション GIF は 1 コマ目だけになる（アイコン用途では許容する）。
 */
@Component
class ImageIoImageNormalizer : ImageNormalizer {
    override fun normalize(
        bytes: ByteArray,
        maxEdge: Int,
    ): ImageNormalizer.NormalizedImage {
        require(bytes.isNotEmpty()) { "empty file" }
        require(maxEdge > 0) { "maxEdge must be positive" }

        // decode の前に画素数で弾く。小さな圧縮ファイルが巨大なビットマップに展開される入力
        // （decompression bomb）は、decode を始めてからでは JVM のメモリを食い潰す。
        val size =
            readSize(bytes)
                ?: throw IllegalArgumentException(
                    "file content is not a supported image (jpeg / png / webp / gif)",
                )
        require(size.pixels <= MAX_PIXELS) {
            "image is too large to process: ${size.width}x${size.height} (max $MAX_PIXELS pixels)"
        }

        // 長辺を maxEdge に収める。元より大きい枠を渡すと Thumbnailator は拡大するため、
        // 枠を元の寸法で頭打ちにして「縮小のみ」にする。
        val scaled =
            Thumbnails
                .of(ByteArrayInputStream(bytes))
                .size(minOf(maxEdge, size.width), minOf(maxEdge, size.height))
                .asBufferedImage()

        val output = ByteArrayOutputStream()
        ImageIO.write(flattenOnWhite(scaled), OUTPUT_FORMAT, output)

        return ImageNormalizer.NormalizedImage(
            bytes = output.toByteArray(),
            contentType = OUTPUT_CONTENT_TYPE,
            extension = OUTPUT_EXTENSION,
        )
    }

    /** アルファを持つ画像を白地に合成し、JPEG が扱える RGB にする。 */
    private fun flattenOnWhite(image: BufferedImage): BufferedImage {
        if (image.type == BufferedImage.TYPE_INT_RGB) return image

        val flattened = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_RGB)
        val graphics = flattened.createGraphics()
        try {
            graphics.color = Color.WHITE
            graphics.fillRect(0, 0, image.width, image.height)
            graphics.drawImage(image, 0, 0, null)
        } finally {
            graphics.dispose()
        }
        return flattened
    }

    /**
     * 画素を展開せずに寸法だけ読む。読めない形式なら null。
     *
     * ImageIO の reader は寸法をヘッダから返せる。ここで弾ければ decode に進まない。
     */
    private fun readSize(bytes: ByteArray): ImageSize? =
        runCatching {
            ImageIO.createImageInputStream(ByteArrayInputStream(bytes))?.use { stream ->
                val readers = ImageIO.getImageReaders(stream)
                if (!readers.hasNext()) return@use null
                val reader = readers.next()
                try {
                    reader.input = stream
                    ImageSize(reader.getWidth(0), reader.getHeight(0))
                } finally {
                    reader.dispose()
                }
            }
        }.getOrNull()

    private data class ImageSize(
        val width: Int,
        val height: Int,
    ) {
        val pixels: Long get() = width.toLong() * height.toLong()
    }

    companion object {
        /** 出力は JPEG 固定。ImageIO が標準で書ける形式で、写真のサイズ効率が良い。 */
        private const val OUTPUT_FORMAT = "jpg"
        private const val OUTPUT_CONTENT_TYPE = "image/jpeg"
        private const val OUTPUT_EXTENSION = "jpg"

        /**
         * 展開後に許す画素数の上限（約 5000x5000 相当）。
         * スマホの写真（最近の機種で 4000x3000 程度）は通り、明らかな細工は落ちる。
         */
        internal const val MAX_PIXELS = 25_000_000L
    }
}

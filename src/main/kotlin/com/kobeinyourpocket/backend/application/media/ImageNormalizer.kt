package com.kobeinyourpocket.backend.application.media

/**
 * アップロードされた画像を保存できる形に整える port（#184）。
 *
 * 利用者が撮った写真をそのまま保存すると 2 つ困る。
 * - EXIF に **GPS 座標**が残る。アイコンは公開 URL で配るため、撮影場所（自宅等）が誰でも読める
 * - 数 MB の画像になり、レビュー一覧で何十件も読むと重い
 *
 * 実装（infrastructure）は decode → 縮小 → 再エンコードする。再エンコードの副作用として
 * EXIF は落ちる。「EXIF だけ消す」より「読める画像だけを作り直す」方が、壊れた入力や
 * 画像に見せかけた別形式も同時に弾ける。
 */
interface ImageNormalizer {
    /**
     * @param bytes アップロードされたバイト列
     * @param maxEdge 出力の長辺の上限（px）。これより小さい画像は拡大しない
     * @throws IllegalArgumentException 画像として読めない / 展開後が大きすぎる場合
     */
    fun normalize(
        bytes: ByteArray,
        maxEdge: Int,
    ): NormalizedImage

    /**
     * 正規化後の画像。
     *
     * [bytes] は配列なので equals / hashCode は参照比較になる。値としての比較はしない。
     */
    data class NormalizedImage(
        val bytes: ByteArray,
        val contentType: String,
        /** オブジェクトキーに使う拡張子（`.` は含まない）。 */
        val extension: String,
    )
}

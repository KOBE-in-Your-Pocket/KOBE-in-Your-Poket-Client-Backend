package com.kobeinyourpocket.backend.application.tourism.query

/**
 * ジャンル 1 件の読み取りビュー（#153）。
 *
 * 表示名は**全言語まとめて**返す。ジャンルはマスタで件数が少なく（現状 5 件）、
 * ADMIN の編集フォームが全言語を必要とする。スポットのように言語ごとへ解決すると、
 * 編集のたびに 4 回取得することになる（`fetchSpotDetail` が実際そうなっている）。
 */
data class GenreView(
    val code: String,
    val displayOrder: Int,
    /** 言語コード → 表示名。全対応言語ぶんが入る。 */
    val labels: Map<String, String>,
    /** このジャンルを参照しているスポットの件数。運営が削除の可否を判断するために返す。 */
    val spotCount: Long,
)

package com.kobeinyourpocket.backend.application.tourism.query

/**
 * スポット詳細の読みモデル（#256）。言語解決済みの [spot] に、詳細画面だけで見せる値を添える。
 *
 * 一覧（[SpotView] の List）には閲覧数を載せない。一覧で使う場面が無く、スポットごとの COUNT が要るため。
 */
data class SpotDetailView(
    val spot: SpotView,
    /** 全期間の閲覧数。閲覧がまだ無いスポットは 0。 */
    val viewCount: Long,
)

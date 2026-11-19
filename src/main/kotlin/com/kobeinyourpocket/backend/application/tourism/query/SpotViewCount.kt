package com.kobeinyourpocket.backend.application.tourism.query

/** スポット 1 件分の閲覧数の読みモデル（#239）。 */
data class SpotViewCount(
    val spotId: String,
    val viewCount: Long,
)

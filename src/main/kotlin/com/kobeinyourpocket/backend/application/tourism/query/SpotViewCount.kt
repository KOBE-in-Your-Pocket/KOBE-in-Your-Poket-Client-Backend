package com.kobeinyourpocket.backend.application.tourism.query

/** スポット 1 件分の閲覧数の読みモデル（#239）。[name] は要求言語で解決済み（#241）。 */
data class SpotViewCount(
    val spotId: String,
    val name: String,
    val viewCount: Long,
)

package com.kobeinyourpocket.backend.domain.tourism.model

/**
 * 観光スポットの評価（5 段階）。
 *
 * feature ① では未集計のため通常 `null`。Client `SpotRating` に対応する。
 */
data class SpotRating(
    val value: Double,
) {
    init {
        require(value in RATING_RANGE) {
            "rating must be between 0 and 5, got $value"
        }
    }

    companion object {
        private val RATING_RANGE = 0.0..5.0
    }
}

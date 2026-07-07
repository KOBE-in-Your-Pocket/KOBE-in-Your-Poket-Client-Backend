package com.kobeinyourpocket.backend.domain.tourism.spot.vo

/**
 * [値オブジェクト] 5 段階評価。
 *
 * feature ① では未集計のため `aggregate.Spot` 上では通常 `null`。
 * Client `SpotRating` に対応する。
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

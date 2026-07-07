package com.kobeinyourpocket.backend.domain.tourism.review.vo

/**
 * [値オブジェクト] レビューの星評価（1〜5 の整数）。
 *
 * [SpotRating] が集計平均（小数）を表すのに対し、本クラスはユーザーが入力する
 * 1〜5 段階の整数評価を表す。[Companion.of] が生成入口。
 */
@JvmInline
value class ReviewRating private constructor(
    val value: Int,
) {
    init {
        require(value in RATING_RANGE) {
            "rating must be between ${RATING_RANGE.first} and ${RATING_RANGE.last}, got $value"
        }
    }

    companion object {
        val RATING_RANGE = 1..5

        fun of(value: Int): ReviewRating = ReviewRating(value)
    }
}

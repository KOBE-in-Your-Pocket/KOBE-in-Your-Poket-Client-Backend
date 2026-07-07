package com.kobeinyourpocket.backend.domain.tourism.spot.vo

/**
 * [値オブジェクト] 画像 URL（言語非依存）。
 *
 * DB `spot.image_url` / Client `SpotMedia` に対応。`aggregate.Spot` がコンポジションで保持する。
 */
data class SpotMedia(
    val imageUrl: String,
) {
    init {
        require(imageUrl.isNotBlank()) { "imageUrl must not be blank" }
    }
}

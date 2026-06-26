package com.kobeinyourpocket.backend.domain.tourism.model

/**
 * 観光スポットのメディア情報（言語非依存）。
 *
 * DB `spot.image_url` に対応。Client `SpotMedia` に対応する。
 */
data class SpotMedia(
    val imageUrl: String,
) {
    init {
        require(imageUrl.isNotBlank()) { "imageUrl must not be blank" }
    }
}

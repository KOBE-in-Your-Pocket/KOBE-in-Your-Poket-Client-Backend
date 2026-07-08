package com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo

/**
 * 避難所のメディア情報（値オブジェクト・言語非依存）。
 *
 * Client `ShelterMedia` / DB `shelter.image_url` に対応する。
 */
data class ShelterMedia(
    val imageUrl: String,
) {
    init {
        require(imageUrl.isNotBlank()) { "imageUrl must not be blank" }
    }
}

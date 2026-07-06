package com.kobeinyourpocket.backend.domain.evacuation.vo

/**
 * 画像 URL の Value Object（言語非依存）。
 *
 * DB `shelter.image_url` / Client `ShelterMedia` に対応する。
 * `aggregate.EvacuationShelter` がコンポジションで保持する。
 */
data class ShelterMedia(
    val imageUrl: String,
) {
    init {
        require(imageUrl.isNotBlank()) { "imageUrl must not be blank" }
    }
}

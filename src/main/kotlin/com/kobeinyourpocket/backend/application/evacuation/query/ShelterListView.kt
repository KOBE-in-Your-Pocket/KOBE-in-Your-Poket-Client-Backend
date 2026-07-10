package com.kobeinyourpocket.backend.application.evacuation.query

/** [GetShelterListService] の戻り値。避難所一覧とデータセット meta を同一スナップショットで束ねる（#85）。 */
data class ShelterListView(
    val shelters: List<ShelterView>,
    val metadata: ShelterDatasetMetadataView,
)

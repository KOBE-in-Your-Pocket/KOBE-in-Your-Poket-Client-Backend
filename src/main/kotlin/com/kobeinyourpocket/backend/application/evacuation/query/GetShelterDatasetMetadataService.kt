package com.kobeinyourpocket.backend.application.evacuation.query

import org.springframework.stereotype.Service

/** 避難所データセット meta 取得ユースケース（read）。domain 集約を経由せず [ShelterDatasetMetadataQuery] port へ委譲する（#85）。 */
@Service
class GetShelterDatasetMetadataService(
    private val shelterDatasetMetadataQuery: ShelterDatasetMetadataQuery,
) {
    fun getMetadata(): ShelterDatasetMetadataView = shelterDatasetMetadataQuery.get()
}

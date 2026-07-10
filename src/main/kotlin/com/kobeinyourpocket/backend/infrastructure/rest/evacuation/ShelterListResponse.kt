package com.kobeinyourpocket.backend.infrastructure.rest.evacuation

import com.kobeinyourpocket.backend.application.evacuation.query.ShelterDatasetMetadataView
import com.kobeinyourpocket.backend.application.evacuation.query.ShelterView
import java.time.Instant
import java.time.LocalDate

/**
 * `GET /api/v1/evacuation/shelters` のレスポンス封筒（#85）。
 *
 * `meta.updatedAt` が Client の起動時差分チェックのキー（要件定義 §4.4 E-2）。
 * `meta.source` / `meta.asOf` は #66 で保持を開始した出典・データ基準日。
 */
data class ShelterListResponse(
    val data: List<ShelterResponse>,
    val meta: ShelterListMetaResponse,
) {
    companion object {
        fun of(
            shelters: List<ShelterView>,
            metadata: ShelterDatasetMetadataView,
        ): ShelterListResponse =
            ShelterListResponse(
                data = shelters.map(ShelterResponse::from),
                meta = ShelterListMetaResponse.from(metadata),
            )
    }
}

data class ShelterListMetaResponse(
    val source: String,
    val asOf: LocalDate,
    val updatedAt: Instant,
) {
    companion object {
        fun from(metadata: ShelterDatasetMetadataView): ShelterListMetaResponse =
            ShelterListMetaResponse(
                source = metadata.source,
                asOf = metadata.asOf,
                updatedAt = metadata.updatedAt,
            )
    }
}

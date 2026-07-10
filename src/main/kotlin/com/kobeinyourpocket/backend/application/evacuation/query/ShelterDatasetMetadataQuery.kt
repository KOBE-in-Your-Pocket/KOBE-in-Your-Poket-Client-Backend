package com.kobeinyourpocket.backend.application.evacuation.query

/** read 専用 port。application が定義し infrastructure.query が実装する（#85）。 */
interface ShelterDatasetMetadataQuery {
    fun get(): ShelterDatasetMetadataView
}

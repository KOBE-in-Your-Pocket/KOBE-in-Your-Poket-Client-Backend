package com.kobeinyourpocket.backend.features.tourism.domain.repository

import com.kobeinyourpocket.backend.features.tourism.domain.aggregate.SpotWithLocalizations

/** write 専用 port（command）。read は [com.kobeinyourpocket.backend.features.tourism.application.query.SpotQuery]。 */
interface SpotRepository {
    fun save(spot: SpotWithLocalizations): SpotWithLocalizations
}

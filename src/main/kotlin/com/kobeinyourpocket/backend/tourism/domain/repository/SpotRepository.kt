package com.kobeinyourpocket.backend.tourism.domain.repository

import com.kobeinyourpocket.backend.tourism.domain.aggregate.SpotWithLocalizations

/** write 専用 port（command）。read は [com.kobeinyourpocket.backend.tourism.application.query.SpotQuery]。 */
interface SpotRepository {
    fun save(spot: SpotWithLocalizations): SpotWithLocalizations
}

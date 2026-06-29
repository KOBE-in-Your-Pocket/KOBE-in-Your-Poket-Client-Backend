package com.kobeinyourpocket.backend.domain.tourism.repository

import com.kobeinyourpocket.backend.domain.tourism.aggregate.SpotWithLocalizations

/** write 専用 port（command）。read は [com.kobeinyourpocket.backend.application.tourism.query.SpotQuery]。 */
interface SpotRepository {
    fun save(spot: SpotWithLocalizations): SpotWithLocalizations
}

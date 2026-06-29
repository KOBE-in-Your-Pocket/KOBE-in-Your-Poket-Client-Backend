package com.kobeinyourpocket.backend.domain.tourism.repository

import com.kobeinyourpocket.backend.domain.tourism.aggregate.SpotWithLocalizations

/**
 * SpotId は呼び出し側で採番済みであること。実装は infrastructure.persistence の adapter（#19）。
 */
interface SpotRepository {
    fun findAll(): List<SpotWithLocalizations>

    fun save(spot: SpotWithLocalizations): SpotWithLocalizations
}

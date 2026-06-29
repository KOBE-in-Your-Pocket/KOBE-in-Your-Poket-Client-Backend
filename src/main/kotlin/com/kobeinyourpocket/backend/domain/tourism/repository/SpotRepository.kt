package com.kobeinyourpocket.backend.domain.tourism.repository

import com.kobeinyourpocket.backend.domain.tourism.aggregate.SpotWithLocalizations

/**
 * Spot 集約の永続化 port（依存性逆転 / §2）。実装は infrastructure.persistence 側（#19）。
 */
interface SpotRepository {
    /** 全 Spot を全言語ローカライズ込みで返す（言語解決は application 層）。 */
    fun findAll(): List<SpotWithLocalizations>

    /** Spot 集約を永続化して返す（SpotId は採番済み前提）。 */
    fun save(spot: SpotWithLocalizations): SpotWithLocalizations
}

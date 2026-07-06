package com.kobeinyourpocket.backend.application.tourism.query

/**
 * 言語解決済みスポットの読みモデル（Client `domain/spot.ts` の解決済み `Spot` 形）。
 *
 * CQRS read 側専用。command 側の集約 [com.kobeinyourpocket.backend.domain.tourism.aggregate.Spot] とは別経路。
 */
data class SpotView(
    val id: String,
    val name: String,
    val genre: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val businessHours: String,
    val categoryLabel: String,
    val imageUrl: String,
    val rating: Double?,
    val address: String,
)

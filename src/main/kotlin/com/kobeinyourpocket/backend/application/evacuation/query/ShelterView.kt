package com.kobeinyourpocket.backend.application.evacuation.query

/**
 * 言語解決済み避難所の読みモデル（Client `EvacuationShelter` 形）。
 *
 * CQRS read 側専用。command 側の集約 [com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.model.EvacuationShelter] とは別経路。
 * type / facilityCategory は Client リテラルのコード文字列で保持する。
 */
data class ShelterView(
    val id: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val type: String,
    val facilityCategory: String,
    val imageUrl: String,
    val capacity: Int?,
    val accessible: Boolean,
    val externalUrl: String?,
)

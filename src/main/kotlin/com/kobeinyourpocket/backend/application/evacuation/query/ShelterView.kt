package com.kobeinyourpocket.backend.application.evacuation.query

/**
 * 言語解決済み避難所の読みモデル（Client `EvacuationShelter` 形）。
 *
 * CQRS read 側専用。command 側の集約 [com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.model.EvacuationShelter] とは別経路。
 * type / siting / 各 suitability / petAcceptance は Client リテラルのコード文字列で保持する。
 *
 * 適否は災害種別ごとに 1 フィールドで持つ（#180）。Map にすると read モデルの段で
 * 「キーが無い場合」を扱うことになり、全種別を必ず返すという契約が緩む。
 */
data class ShelterView(
    val id: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val type: String,
    val siting: String,
    val suitabilityLandslide: String,
    val suitabilityFlood: String,
    val suitabilityTsunami: String,
    val suitabilityLargeFire: String,
    val petAcceptance: String,
    val phoneNumber: String?,
    /** 元データの備考（言語解決済み）。避難の判断に関わるため表示する。 */
    val note: String?,
)

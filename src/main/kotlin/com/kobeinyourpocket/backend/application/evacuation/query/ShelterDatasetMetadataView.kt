package com.kobeinyourpocket.backend.application.evacuation.query

import java.time.Instant
import java.time.LocalDate

/**
 * 避難所データセット全体の meta（出典・データ基準日・最終更新日時）の読みモデル（#66 / #85）。
 *
 * `shelter_dataset_metadata`（シングルトン）に対応する。Client はこの `updatedAt` を
 * 起動時のバージョン比較キーとして使い、変化時のみ一覧を全件再取得して `replaceAll` する
 * （真の差分配信はしない / 要件定義 §4.4 E-2）。
 */
data class ShelterDatasetMetadataView(
    val source: String,
    val asOf: LocalDate,
    val updatedAt: Instant,
)

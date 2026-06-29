package com.kobeinyourpocket.backend.features.tourism.domain.vo

/**
 * スポットの言語別ローカライズ内容の Value Object。
 *
 * 単一言語ぶんの言語依存フィールドをまとめる。言語非依存ベースは `aggregate.Spot` 側に持ち、
 * 本オブジェクトは [SpotLocalizations] 経由で言語ごとに保持される（§7.1 の i18n 分割）。
 * DB `spot_localization(name, category_label, description, business_hours)` の 1 行に対応。
 * `businessHours` はモックで言語側（"24時間"/"Open 24 hours"）なのでここに置く。
 */
data class SpotLocalization(
    val name: String,
    val categoryLabel: String,
    val description: String,
    val businessHours: String,
) {
    init {
        require(name.isNotBlank()) { "name must not be blank" }
        require(categoryLabel.isNotBlank()) { "categoryLabel must not be blank" }
    }
}

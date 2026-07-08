package com.kobeinyourpocket.backend.application.manner.query

/**
 * 言語解決済みマナー項目の読みモデル（Client `domain/manner-item.ts` の `MannerItem` 形）。
 *
 * CQRS read 側専用。command 側の集約 [com.kobeinyourpocket.backend.domain.manner.manneritem.model.MannerItem] とは別経路。
 * kind / scope は Client リテラル（`manner|rule` / `local|japan`）のコード文字列で保持する。
 */
data class MannerItemView(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val kind: String,
    val scope: String,
    val relatedSpotIds: List<String>,
)

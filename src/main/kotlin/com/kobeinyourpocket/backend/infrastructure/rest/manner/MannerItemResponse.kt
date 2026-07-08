package com.kobeinyourpocket.backend.infrastructure.rest.manner

import com.kobeinyourpocket.backend.application.manner.query.MannerItemView

/**
 * `GET /api/v1/manner/items` のレスポンス（Client `MannerItem` 型に一致 / 要件定義 §4.5）。
 *
 * kind は `manner|rule`、scope は `local|japan` のリテラル。icon はアイコン識別キー（M-3）。
 * relatedSpotIds は Tourism `Spot.id` への ID 参照のみ（M-2）。
 */
data class MannerItemResponse(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val kind: String,
    val scope: String,
    val relatedSpotIds: List<String>,
) {
    companion object {
        fun from(view: MannerItemView): MannerItemResponse =
            MannerItemResponse(
                id = view.id,
                title = view.title,
                description = view.description,
                icon = view.icon,
                kind = view.kind,
                scope = view.scope,
                relatedSpotIds = view.relatedSpotIds,
            )
    }
}

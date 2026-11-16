package com.kobeinyourpocket.backend.application.tourism.query

import java.time.Instant

/**
 * お気に入り 1 件分の読みモデル（#237）。
 *
 * スポットの名前や画像は持たない。Client は取得済みのスポット一覧と [spotId] で突き合わせて表示する
 * （Client#789 の取り決め。多言語化を Client 側の 1 か所にまとめ、応答も軽くするため）。
 */
data class FavoriteView(
    val spotId: String,
    val createdAt: Instant,
)

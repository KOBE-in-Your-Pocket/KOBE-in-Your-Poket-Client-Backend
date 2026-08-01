package com.kobeinyourpocket.backend.domain.tourism.spot.repository

import com.kobeinyourpocket.backend.domain.tourism.spot.model.Spot
import com.kobeinyourpocket.backend.domain.tourism.spot.model.SpotWithLocalizations
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId

/** [リポジトリ] write 専用 port（command）。read は [com.kobeinyourpocket.backend.application.tourism.query.SpotQuery]。 */
interface SpotRepository {
    /**
     * 更新前の存在確認・不変フィールド（rating 等）の引き継ぎ用に言語非依存ベースだけを取得する。
     * 該当 [id] が無ければ null。ローカライズは更新リクエストで全差し替えされるため読まない。
     */
    fun findSpotById(id: SpotId): Spot?

    fun save(spot: SpotWithLocalizations): SpotWithLocalizations
}

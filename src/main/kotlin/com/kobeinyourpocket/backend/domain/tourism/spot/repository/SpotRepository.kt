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

    /**
     * [findSpotById] と同じものを **行ロック付き**で取得する。該当 [id] が無ければ null。
     *
     * 読み取りから [save] までを 1 つのトランザクションで直列化するために使う。ロックは呼び出し側の
     * トランザクションが終わるまで保持されるため、**トランザクション内から呼ぶこと**（実装はトランザクション必須）。
     * 同じスポットへの同時更新で、後勝ちの上書きや、古い読み取りに基づく画像の誤削除が起きるのを防ぐ。
     */
    fun findSpotByIdForUpdate(id: SpotId): Spot?

    fun save(spot: SpotWithLocalizations): SpotWithLocalizations

    fun existsById(id: SpotId): Boolean

    /** spot_localization・review は ON DELETE CASCADE で連動削除される（V1 / V2）。 */
    fun deleteById(id: SpotId)
}

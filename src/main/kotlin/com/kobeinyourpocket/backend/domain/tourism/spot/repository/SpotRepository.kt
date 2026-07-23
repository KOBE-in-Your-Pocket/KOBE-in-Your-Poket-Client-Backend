package com.kobeinyourpocket.backend.domain.tourism.spot.repository

import com.kobeinyourpocket.backend.domain.tourism.spot.model.SpotWithLocalizations
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId

/** [リポジトリ] write 専用 port（command）。read は [com.kobeinyourpocket.backend.application.tourism.query.SpotQuery]。 */
interface SpotRepository {
    fun save(spot: SpotWithLocalizations): SpotWithLocalizations

    fun existsById(id: SpotId): Boolean

    /** spot_localization・review は ON DELETE CASCADE で連動削除される（V1 / V2）。 */
    fun deleteById(id: SpotId)
}

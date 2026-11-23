package com.kobeinyourpocket.backend.application.tourism.query

import com.kobeinyourpocket.backend.application.tourism.SpotNotFoundException
import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId
import org.springframework.stereotype.Service

/**
 * 詳細取得ユースケース（read）。存在しなければ [SpotNotFoundException]。
 *
 * 閲覧数（#256）も一緒に返す。スポットが無ければ閲覧数は数えずに 404 にする。
 */
@Service
class GetSpotService(
    private val spotQuery: SpotQuery,
    private val spotViewCountQuery: SpotViewCountQuery,
) {
    fun getSpot(
        id: SpotId,
        language: Language,
    ): SpotDetailView {
        val spot = spotQuery.findByIdResolved(id, language) ?: throw SpotNotFoundException(id)
        return SpotDetailView(spot = spot, viewCount = spotViewCountQuery.countBySpotId(id))
    }
}

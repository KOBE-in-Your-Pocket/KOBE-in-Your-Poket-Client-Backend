package com.kobeinyourpocket.backend.application.tourism.query

import com.kobeinyourpocket.backend.domain.tourism.localization.Language
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId
import org.springframework.stereotype.Service

/** 詳細取得ユースケース（read）。存在しなければ [SpotNotFoundException]。 */
@Service
class GetSpotService(
    private val spotQuery: SpotQuery,
) {
    fun getSpot(
        id: SpotId,
        language: Language,
    ): SpotView = spotQuery.findByIdResolved(id, language) ?: throw SpotNotFoundException(id)
}

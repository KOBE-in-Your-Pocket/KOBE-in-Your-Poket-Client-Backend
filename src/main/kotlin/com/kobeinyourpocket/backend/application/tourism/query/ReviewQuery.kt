package com.kobeinyourpocket.backend.application.tourism.query

import com.kobeinyourpocket.backend.domain.tourism.vo.Language
import com.kobeinyourpocket.backend.domain.tourism.vo.SpotId

/** read 専用 port。application が定義し infrastructure.query が実装する。 */
interface ReviewQuery {
    fun findBySpot(
        spotId: SpotId,
        language: Language,
    ): List<ReviewView>
}

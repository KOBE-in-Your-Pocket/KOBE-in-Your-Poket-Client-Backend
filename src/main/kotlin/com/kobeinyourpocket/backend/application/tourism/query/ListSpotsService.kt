package com.kobeinyourpocket.backend.application.tourism.query

import com.kobeinyourpocket.backend.domain.tourism.vo.Language
import org.springframework.stereotype.Service

/** 一覧取得ユースケース（read）。domain 集約を経由せず [SpotQuery] port へ委譲する。 */
@Service
class ListSpotsService(
    private val spotQuery: SpotQuery,
) {
    fun listSpots(language: Language): List<SpotView> = spotQuery.findAllResolved(language)
}

package com.kobeinyourpocket.backend.domain.tourism.spot.model

import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotLocalizations

/**
 * [集約] 言語非依存ベース [Spot] と全言語ローカライズを束ねた完全形（永続化単位 / 言語解決前）。
 */
data class SpotWithLocalizations(
    val spot: Spot,
    val localizations: SpotLocalizations,
)

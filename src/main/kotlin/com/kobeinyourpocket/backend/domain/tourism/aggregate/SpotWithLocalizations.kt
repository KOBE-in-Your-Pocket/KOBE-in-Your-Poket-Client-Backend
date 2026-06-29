package com.kobeinyourpocket.backend.domain.tourism.aggregate

import com.kobeinyourpocket.backend.domain.tourism.vo.SpotLocalizations

/**
 * 言語非依存ベース [Spot] と全言語ローカライズを束ねた Spot 集約の完全形（永続化単位 / 言語解決前）。
 */
data class SpotWithLocalizations(
    val spot: Spot,
    val localizations: SpotLocalizations,
)

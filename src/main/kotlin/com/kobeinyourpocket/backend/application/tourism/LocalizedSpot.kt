package com.kobeinyourpocket.backend.application.tourism

import com.kobeinyourpocket.backend.domain.tourism.aggregate.Spot
import com.kobeinyourpocket.backend.domain.tourism.vo.SpotLocalization

/** 要求言語へ解決済みのスポット（base + 単一言語）。web 層が Client `Spot` 形へ写像する。 */
data class LocalizedSpot(
    val spot: Spot,
    val localization: SpotLocalization,
)

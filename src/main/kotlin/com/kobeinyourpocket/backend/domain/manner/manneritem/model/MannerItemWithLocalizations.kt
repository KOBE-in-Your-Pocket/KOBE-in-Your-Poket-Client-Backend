package com.kobeinyourpocket.backend.domain.manner.manneritem.model

import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerLocalizations

/**
 * [集約] 言語非依存ベース [MannerItem] と全言語ローカライズを束ねた完全形（永続化単位 / 言語解決前）。
 */
data class MannerItemWithLocalizations(
    val item: MannerItem,
    val localizations: MannerLocalizations,
)

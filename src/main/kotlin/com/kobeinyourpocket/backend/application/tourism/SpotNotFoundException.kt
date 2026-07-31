package com.kobeinyourpocket.backend.application.tourism

import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId

/** 指定 [SpotId] のスポットが存在しない場合の例外（REST では 404 / #83）。 */
class SpotNotFoundException(
    id: SpotId,
) : RuntimeException("Spot not found: ${id.value}")

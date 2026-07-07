package com.kobeinyourpocket.backend.application.tourism.query

import com.kobeinyourpocket.backend.domain.tourism.localization.Language
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId

/** read 専用 port。application が定義し infrastructure.query が実装する。 */
interface SpotQuery {
    fun findAllResolved(language: Language): List<SpotView>

    /** 該当 [id] が無ければ null（存在確認は呼び出し側の責務）。 */
    fun findByIdResolved(
        id: SpotId,
        language: Language,
    ): SpotView?
}

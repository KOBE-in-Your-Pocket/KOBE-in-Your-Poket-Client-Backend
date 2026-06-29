package com.kobeinyourpocket.backend.tourism.application.query

import com.kobeinyourpocket.backend.tourism.domain.vo.Language

/** read 専用 port。application が定義し infrastructure.query が実装する。 */
interface SpotQuery {
    fun findAllResolved(language: Language): List<SpotView>
}

package com.kobeinyourpocket.backend.application.tourism.query

import com.kobeinyourpocket.backend.domain.tourism.localization.Language

/** read 専用 port。application が定義し infrastructure.query が実装する。 */
interface SpotQuery {
    fun findAllResolved(language: Language): List<SpotView>
}
